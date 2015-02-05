class Vector
{
	public double x = 0, y = 0, z = 0;
	public boolean render = true;
	static int count = 0;
	public Vector(int x, int y, int z)
	{
		this.x = x;
		this.y = -y;	// INCONSISTANT
		this.z = z;
		count++;
	}
	public Vector(double x, double y, double z)
	{
		this.x = x;
		this.y = y;		// INCONSISTANT
		this.z = z;
		count++;
	}
	public static Vector smul(Vector v, double s)
	{
		return new Vector(v.x*s, v.y*s, v.z*s);
	}
	public void osmul(double s)
	{
		x *= s;
		y *= s;
		z *= s;
	}
	public static Vector sadd(Vector v, double s)
	{
		return new Vector(v.x+s, v.y+s, v.z+s);
	}
	public static Vector add(Vector a, Vector b)
	{
		return new Vector(a.x+b.x, a.y+b.y, a.z+b.z);
	}
	public static Vector sub(Vector a, Vector b)
	{
		return new Vector(a.x-b.x, a.y-b.y, a.z-b.z);
	}
	public void osub(Vector b)
	{
		x-=b.x;
		y-=b.y;
		z-=b.z;
	}
	public static Vector mul(Vector a, Vector b)
	{
		return new Vector(a.x*b.x, a.y*b.y, a.z*b.z);
	}
	public static double dot(Vector a, Vector b)
	{
		return a.x*b.x + a.y*b.y + a.z*b.z;
	}
	public static Vector cross(Vector a, Vector b)
	{
		return new Vector(a.y*b.z-a.z*b.y, a.z*b.x - a.x*b.z, a.x*b.y - a.y*b.x);
	}
	public Vector deepCopy()
	{
		return new Vector(x, y, z);
	}
	public Vector norm()
	{
		return new Vector(x/z, y/z, 1);
	}
	public void rotateZ(double degrees, Vector center)
	{
		x-=center.x;
		z-=center.z;
		double tempx = x;
		x = x*Math.cos(degrees)-z*Math.sin(degrees);
		z = tempx*Math.sin(degrees)+z*Math.cos(degrees);
		z+=center.z;
		x+=center.x;
	}
	public void rotateX(double degrees, Vector center)
	{
		y-=center.y;
		z-=center.z;
		double tempy = y;
		y = y*Math.cos(degrees)-z*Math.sin(degrees);
		z = tempy*Math.sin(degrees)+z*Math.cos(degrees);
		z+=center.z;
		y+=center.y;
	}
	public String toString()
	{
		return "<" + x + ", " + y + ", " + z + ">";
	}
	public void swap()
	{
		double tempx = x;
		x = z;
		z = tempx;
	}
}

class SharedVector extends Vector
{
	boolean calculated = false;
	boolean rcalc = false;
	Vector crcalcs = paper.origin;
	Vector v = paper.origin;
	int count = 0;
	public SharedVector(int x, int y, int z)
	{
		super(x, y, z);
	}
	public SharedVector(double x, double y, double z)
	{
		super(x, y, z);
	}
	public SharedVector deepCopy()
	{
		return this;
	}
	public static SharedVector smul(Vector v, double s)
	{
		return new SharedVector(v.x*s, v.y*s, v.z*s);
	}
}
