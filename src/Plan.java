
/**
 *
 * @authors Andrei Tapai, Andre Paiva
 */
public class Plan
{
    public int num;
    public double selectivity;
    public boolean noBranch;
    public double cost;
    public int left, right;
    public int bitmap;
    public double cmetric;

    public Plan(int n, double p, boolean b, double c, int l, int r)
    {
        num = n;
        selectivity = p;
        noBranch = b;
        cost = c;
        left = l;
        right = r;
        cmetric = (p-1.0)/cost; //this will be updated
        bitmap = 0;
    }
}