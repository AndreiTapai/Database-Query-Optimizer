import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 *
 * @authors Andrei Tapai, Andre Paiva
 */
public class Main 
{
    public static double r,t,l,m,a,f;
    
    public static ArrayList<Double[]> querySelectivities;
    public static Double[] currentQuery;
    public static int debug = 0;
    
    public static void main(String[] args)
    {
        try
        {
            readQueryFile(args[0]);
            readConfigFile(args[1]);
        } 
        catch (Exception e)
        {}
        
        for (Double[] querySelectivity : querySelectivities) {
            currentQuery = querySelectivity;
            algorithm411();
        }
    }
    
    public static void readQueryFile(String queryFile) throws java.io.FileNotFoundException 
    {
        ArrayList<Double[]> input = new ArrayList<Double[]>();
        File file = new File(queryFile);
        Scanner scanner = new Scanner(file);
        
        while (scanner.hasNextLine())
        {
            String line = scanner.nextLine();
            String[] split = line.split(" ");

            Double[] temp = new Double[split.length];
            for (int i = 0; i < split.length; i++)
            {
                temp[i] = Double.parseDouble(split[i]);
            }

            input.add(temp);
        }
        
        querySelectivities = input;
    }
    
    public static void readConfigFile(String configFile) throws java.io.FileNotFoundException
    {
        File file = new File(configFile);
        Scanner scanner = new Scanner(file);
        
        while (scanner.hasNextLine())
        {
            String line = scanner.nextLine();
            String[] split = line.split(" ");
            
            if (split[0].equalsIgnoreCase("r"))
            {
                r = Double.parseDouble(split[2]);
            }
            else if (split[0].equalsIgnoreCase("t"))
            {
                t = Double.parseDouble(split[2]);
            }
            else if (split[0].equalsIgnoreCase("l"))
            {
                l = Double.parseDouble(split[2]);
            }
            else if (split[0].equalsIgnoreCase("m"))
            {
                m = Double.parseDouble(split[2]);
            }
            else if (split[0].equalsIgnoreCase("a"))
            {
                a = Double.parseDouble(split[2]);
            }
            else if (split[0].equalsIgnoreCase("f"))
            {
                f = Double.parseDouble(split[2]);
            }
            else
            {}
        }
    }
    
    public static void algorithm411()
    {     
        int k = currentQuery.length;
        int numPlans = (int)(Math.pow(2,k)) - 1;
        Plan[] planArray = new Plan[numPlans];
        ArrayList<ArrayList<Integer>> powerSet  = getBinaryPowerSet(numPlans);

        if (debug == 1)
        {
            System.out.println(powerSet.toString());
        }
        constructPlans(planArray, powerSet);

        if (debug == 1)
        {
            System.out.println(Arrays.toString(planArray));
        }
        
        for (int R = 0; R < planArray.length; R++)
        {
            for (int L = 0; L < planArray.length; L++)
            {
                int right = planArray[R].bitmap;
                int left = planArray[L].bitmap;
                
                if (emptySetIntersect(right,left))
                {
                    planArray[L].cmetric = computeCMetric(planArray, L);
                    planArray[R].cmetric = computeCMetric(planArray, R);
                    
                   /* if (the c-metric of s1 is dominated by the c-metric of the leftmost &-term in s) then
                    {do nothing; suboptimal by Lemma 4.8}
                    else if (A[s1].p <= 1/2 and the d-metric of s1 is dominated by the d-metric of some
                    other &-term in s) then
                    {do nothing; suboptimal by Lemma 4.9}
                    else {
                    Calculate the cost c for the combined plan (s1 && s) using Eq. (1). If c < A[s1 union s].c */
                    if (!(planArray[L].cmetric <= planArray[R].cmetric) ||
                        !(planArray[L].selectivity <= 0.5 && computeFCost(planArray[L]) 
                            < computeFCost(planArray[R])))
                    {
                        /* fcost(E)+ mq + pC (1)
                        where p is the overall combined selectivity of E, 
                        q = min(p, 1-p), and C is the cost of the plan P2 */
                        double fcostE = computeFCost(planArray[L]);
                        double p = planArray[L].selectivity;
                        double q = Math.min(p, 1.0 - p);
                        double pC = p * planArray[R].cost;
                        //Final Equation
                        double combinedCost = fcostE + m*q + pC;

                        /* If c < A[s1 union s].c
                        then:
                        (a) Replace A[s1 union s].c with c.
                        (b) Replace A[s1 union s].L with s1.
                        (c) Replace A[s1 union s].R with s. */
                        int leftRightUnion = getPlanUnion(planArray,right+left);
                        if (combinedCost < planArray[leftRightUnion].cost)
                        {
                            planArray[leftRightUnion].cost = combinedCost;
                            planArray[leftRightUnion].left = L;
                            planArray[leftRightUnion].right = R;
                        }
                    }
                }
            }
        }
        /* At the end of the algorithm, A[S].c contains the optimal cost, 
        and its corresponding plan can be recursively derived by combining 
        the &-conjunction A[S].L to the plan for A[S].R via &&. */
        Plan optimalCost = planArray[planArray.length - 1];
        
        if (debug == 1)
        {
            System.out.println(optimalCost.cost + " " + optimalCost.left
                                + " " + optimalCost.right);
        }

        ArrayList<Plan> finalPlan = new ArrayList<Plan>();

        buildPlan(planArray, finalPlan, planArray.length - 1);

        prettyPrintCCode(finalPlan, optimalCost);
    }    

    /*This implementation works on idea that each element in the original set can either be in the power set or not in it. 
    With n elements in the original set, each combination can be represented by a binary string of length n. To get all possible combinations, 
    all you need is a counter from 0 to 2n - 1. If the kth bit in the binary string is 1, the kth element of the original set is in this combination. 
    http://rosettacode.org/wiki/Power_set#Java (under Binary String)*/
    public static ArrayList<ArrayList<Integer>> getBinaryPowerSet(int setSize)
    {
        ArrayList<ArrayList<Integer>> powerSet = new ArrayList<ArrayList<Integer>>(setSize);

        for (int i = 1; i <= setSize; i++)
        {
            ArrayList<Integer> subSet = new ArrayList<Integer>();
            String bitMap = Integer.toBinaryString(i);
         
            for (int k = 0; k < bitMap.length(); k++)
            {
                String s = bitMap.substring(bitMap.length() - 1 - k, bitMap.length() - k );
                if (s.equalsIgnoreCase("1"))
                {
                    subSet.add(k);
                }
            }
            powerSet.add(subSet);
        }
        
        return powerSet;
    }
    
    //For pretty-printing code
    public static ArrayList<Integer> getBinarySet(int bitmap)
    {
        ArrayList<Integer> indicesofSelectivities = new ArrayList<Integer>();
        String bitMap = Integer.toBinaryString(bitmap);
        for (int i = 0; i < bitMap.length(); i++)
        {
            String s = bitMap.substring(bitMap.length() - 1 - i, bitMap.length() - i);
            if (s.equalsIgnoreCase("1"))
            {
                indicesofSelectivities.add(i);
            }            
        }
        return indicesofSelectivities;
    }

    public static void constructPlans(Plan[] planArray, ArrayList<ArrayList<Integer>> powerSet)
    {
        for (int i = 0; i < powerSet.size(); i++)
        {
            ArrayList<Integer> indexes = powerSet.get(i);

            double costLogicalAnd = computeLogicalAnd(indexes);
            double costNoBranch = computeNoBranch(indexes);

            boolean noBranch = false;
            double cost = costLogicalAnd;
            if (costNoBranch < costLogicalAnd)
            {
                cost = costNoBranch;
                noBranch = true;
            }

            double p = multiplySelectivities(indexes);
            planArray[i] = new Plan(indexes.size(), p, noBranch, cost, -1, -1);

            planArray[i].bitmap = i+1;
        }
    }

    public static double computeCMetric(Plan[] p, int index)
    {      
        return (p[index].selectivity - 1.0) / computeFCost(p[index]);
    }

    public static double multiplySelectivities(ArrayList<Integer> indexes)
    {
        double p = 1.0;
        for (Integer i: indexes)
        {
            double tempP = currentQuery[i];
            p *= tempP;
        }
        return p;
    }

    public static double computeNoBranch(ArrayList<Integer> indexes)
    {
        double k = (double) indexes.size();
        //Example 4.4
        return ((k * r) + ((k - 1.0) * l) + (k * f) + a);
    }

    public static double computeLogicalAnd(ArrayList<Integer> indexes)
    {
        double k = (double)indexes.size();

        double p = multiplySelectivities(indexes);
        //Example 4.5
        double q = p;
        if (p >= .5)
        { 
            q = 1 - p; 
        }

        return ((k * r) + ((k - 1.0) * l) + (k * f) + t + (m * q) + (p * a));
    }

    public static double computeFCost(Plan plan)
    {
        double k = (double) plan.num;
        //Example 4.7
        return ((k * r) + ((k - 1.0) * l) + (k * f) + t);
    }

    public static boolean emptySetIntersect(int a, int b)
    {
        /*
        String ca = Integer.toBinaryString(a);
        String cb = Integer.toBinaryString(b);
        System.out.println(ca);
        System.out.println(cb);
        
        for (int i = 0; i < ca.length() || i < cb.length(); i++)
        {
            if (ca.charAt(i) == '1' && cb.charAt(i) == '1')
            {
                return false;
            }
        }
        return true;
        */
        int xorAB = a^b;
        if (xorAB == a + b)
        {
            return true;
        }
        return false;
    }

    public static int getPlanUnion(Plan[] plan, int n)
    {
        for (int i = 0; i < plan.length; i++)
        {
            if (plan[i].bitmap == n)
            {
                return i;
            }
        }
        return 0;
    }

    public static void buildPlan(Plan[] planArray, ArrayList<Plan> finalPlan, int nextIndex) 
    {
        Plan currentPlan = planArray[nextIndex];
        if(currentPlan.left < 0 && currentPlan.right < 0) 
        {
            finalPlan.add(planArray[nextIndex]);
        }
        else 
        {
            if(currentPlan.left >= 0)
            {
                planArray[currentPlan.left].noBranch = false;
                //in-order traversal through left sub-tree
                buildPlan(planArray, finalPlan, currentPlan.left);
            }
            if(currentPlan.right >= 0)
            {
                //in-order traversal through right sub-tree
                buildPlan(planArray, finalPlan, currentPlan.right);
            }
        }
    }

    public static void prettyPrintCCode(ArrayList<Plan> finalPlan, Plan optimalCost)
    {
        String conditionals = "";
        String noBranch = "";
        String selectivityList = "";
        
        for (Double d: currentQuery)
        {
            selectivityList += d + " ";
        }
        for (Plan r: finalPlan)
        {
            String conditionalChain = "";
            ArrayList<Integer> indexes = getBinarySet(r.bitmap);
            for (Integer indice : indexes) 
            {
                int num = indice + 1;

                if (conditionalChain.length() > 0)
                { 
                    conditionalChain += " & ";
                }               
                conditionalChain += "t" + num + "[o" + num + "[i]]";
                
                if (debug == 1)
                {
                    System.out.println(conditionalChain);
                }
            }
            if (r.noBranch)
            {
                if (noBranch.length() > 0)
                { 
                    noBranch += " & ";
                }
                noBranch += conditionalChain;
            } 
            else
            {
                if (conditionals.length() > 0)
                { 
                    conditionals += " && ";
                }
                conditionals += "(" + conditionalChain + ")";
            }
        }
        
        String conditional = "if(" + conditionals + ") {\n";

        String ifExpression = "\tanswer[j] = i;\n\tj += (" + noBranch + ");";
        
        if (debug == 1)
        {
            System.out.println(conditional);
            System.out.println(ifExpression);
        }
        
        if (noBranch.length() == 0)
        {
            ifExpression = "answer[j++] = i;";
        }

        if (conditionals.length() == 0)
        {
            System.out.println("==================================================================");
            System.out.println(selectivityList);
            System.out.println("------------------------------------------------------------------");
            System.out.println(ifExpression.replace("\t",""));
            System.out.println("------------------------------------------------------------------");
            System.out.println("Cost: " + optimalCost.cost);            
        } 
        else
        {
            System.out.println("==================================================================");
            System.out.println(selectivityList);
            System.out.println("------------------------------------------------------------------");            
            System.out.println(conditional + ifExpression + "\n}");
            System.out.println("------------------------------------------------------------------");
            System.out.println("Cost: " + optimalCost.cost);                        
        }           
    }
}
