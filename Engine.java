import javax.swing.*;	// Import needed for window ('JFrame') and 'JComponent's
import java.awt.event.*;	// Import needed for mouse listener
import java.util.*;	// Import needed for 'ArrayList's
import java.awt.*; 	// Import needed for 'Color's
import java.io.*;	// Import needed for 'File's and associated classes 
import java.awt.image.*;	// Import needed for 'BufferedImage's
import java.util.concurrent.*;	// Import needed for 'CopyOnWriteArrayList's and scheduling of window updates through 'ScheduledThreadPoolExecutor'
import javax.imageio.*;	// Import needed for reading image files
import java.lang.reflect.*;	// Import needed to call methods in other programs that have registered themselves to recieve information from the renderer
import java.awt.geom.*;

public class Engine	// Class declaration
{
public static JFrame frame;	// Declaring the window that the renderer will draw to
static Cursor BLANK_CURSOR = Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "JREN_BLANK");
static Cursor NORMAL_CURSOR = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	public static void main(String[] args)
	{
		new Engine(new paper());	// Creating a canvas to draw on, and passing it to a new instance of Engine so we are no longer in a static context.
	}
	public Engine(paper papr)
	{
		try	// Telling Java that we will do our own error handling
		{
			frame = new JFrame();	// Creating a window
			frame.setUndecorated(true);	// Getting rid of the Windows taskbar at the top of the window so we can draw on the whole thing
			frame.setVisible(true);	// Making the window visible.
			frame.setSize(paper.iwidth, paper.iheight);	// Setting the frame to the size specified by the canvas
			frame.setResizable(false);	// Forcing it to remain at that size so we don't need to recalculate anything
			frame.setLocationRelativeTo(null);	// Moving it to the center of the screen
			frame.setTitle("Render");	// Setting the title of the window
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	// Telling Java to terminate the program when the windo is closed
			frame.add(papr, BorderLayout.CENTER);	// Adding the canvas to the center of the frame
			frame.getContentPane().setCursor(BLANK_CURSOR);
			new ScheduledThreadPoolExecutor(5).scheduleAtFixedRate(new Animator(frame), 0l, 2l, TimeUnit.MILLISECONDS);	// Telling Java to poll the Animator class to update the screen every 10 milliseconds
		}
		catch(Exception e)	// Not actually doing anything if an error is thrown   <- BAD PRACTICE >:|
		{}
	}
}

class paper extends JComponent implements MouseListener	// Making the paper class act as both a component to draw on,
														// and a listener that is notified whenever the mouse is moved or clicked
{
	static BufferedImage canvas, canvas2;	// Creating the two images that we are using as buffers (storing the image while we are editing it)
	static ArrayList<Triangle> tris = new ArrayList<Triangle>();	// Creating a master list of all the Triangles in the scene
	static ArrayList<Light> lights = new ArrayList<Light>();	// Creating a master list of all the Lights in the scene
	static int iwidth = Toolkit.getDefaultToolkit().getScreenSize().width;	// Polling the OS for the width of the screen
	static double camz = -iwidth/3; // Setting the distance from the virtual camera to the screen (directly affects FOV) based upon the width of the screen
	static double camx = 0;	// Setting the x offset of the camera to zero (used in 3D rendering as eye seperation)
	static int iheight = Toolkit.getDefaultToolkit().getScreenSize().height;	// Polling the OS for the height of the screen
	static int mxrot = 0, myrot = 0;	// Declaring variables for handling camera rotation as caused by the user (as in using the mouse to look around)
	static Vector camPos = new Vector(0, 0, 0);	// Declaring the position of the camera in the scene
	static Robot r;	// Declaring a Robot to allow control of the mouse (to keep it rooted at the center of the screen)
	static boolean texture = true;	// Declaring a boolean to keep track of whether or not textures are enabled
	static boolean light = true;	// Declaring a boolean to keep track of whether or not lights are enabled
	static boolean antialias = true;	// Declaring a boolean to keep track of whether or not antialiasing is enabled
	static boolean fps = true;	// Declaring a boolean to keep track of whether or not displaying fps is enabled
	static boolean mouse = true;	// Declaring a boolean to keep track of whether or not using the mouse to rotate the camera is enabled
	static boolean lines = true;	// Declaring a boolean to keep track of whether or not to outline polygons
	static boolean rift = false;	// Declaring a boolean to keep track of whether or not barrel distortion is enabled
	static boolean cregs = false;	// Declaring a boolean to keep track of whether or not another program has requested access to mouse clicks
	static boolean dregs = false;	// Declaring a boolean to keep track of whether or not another program has requested access to draw on the screen
	static boolean d3 = true;	// Declaring a boolean to keep track of whether or not SBS 3D is enabled
	static int mxa = Toolkit.getDefaultToolkit().getScreenSize().width/2;	// Finding the center of the screen so we know where to snap the mouse back to
	static int mya = Toolkit.getDefaultToolkit().getScreenSize().height/2;	//		Polling for the screen size in case iwidth or iheight have been modified
	static Method cregm;	// Declaring a 'Method' object to store the method to be called when the mouse is clicked if any method is registered to do so
	static Method dregm;	// Declaring a 'Method' object to store the method to be called when the screen is updated if any method is registered to do so
	static double psc = 1;	// Setting the scaling needed to render to the full window size. 1 : Full Resolution, >1 : Lower resolution, <1 : Causes problems.
	static final Vector origin = new Vector(0, 0, 0);
	static Color bgcolor = new Color(100, 100, 100, 255);
	static int cwidth;
	static int cheight;
	static int tval = 0;
	static Vector qstor;
	static Timer timer = new Timer();
	static int lastTime = 0;
	public paper()	// Declaring a constructor for the 'paper' class (otherwise referred to as the "canvas" in comments)
	{
		try	// Telling Java that we will do our own error handling
		{
			cwidth = (int)(iwidth*psc);
			cheight = (int)(iheight*psc);
			addMouseListener(this);	// Telling Java to alert 'paper' whenever the mouse is moved of clicked
			if (d3) // Checking to see if 3D is enabled, if so:
			{
				canvas = new BufferedImage((int)(iwidth/2/psc), (int)(iheight/psc), BufferedImage.TYPE_INT_RGB);	// Creating an image to cover the left half of the screen
				canvas2 = new BufferedImage((int)(iwidth/2/psc), (int)(iheight/psc), BufferedImage.TYPE_INT_RGB);	// and one to cover the right half
			}
			else	// If 3D is disabled:
				canvas = new BufferedImage((int)(iwidth/psc), (int)(iheight/psc), BufferedImage.TYPE_INT_RGB);	// Creating one image to cover the entire screen
			
			r = new Robot();	// Creating the robot that will be used to control the mouse
			r.mouseMove(mxa, mya);	// Moving the mouse to the center of the screen where it will be anchored, so we have a known starting point
		}
		catch(Exception e)	// Not actually doing anything if an error is thrown   <- BAD PRACTICE >:|
		{}
	}

	public void paint(Graphics g)	// Method called every time the window is updated, deals with "painting" the image on the canvas
	{
		Graphics2D g2 = (Graphics2D)g;	// Casting the canvas' 'Graphics' object into a more useable form
		Graphics2D g22 = (Graphics2D)canvas.getGraphics();	// Getting the graphics for the first (and possible only) image
		g22.setPaint(bgcolor);	// Setting the paint to gray
		g22.fillRect(0, 0, iwidth, iheight);	// Filling in the entire first image with gray to overwrite the old data it contained
		g22.dispose();	// Telling Java that we are finished messing with the image's graphics
		if (d3)	// If 3D is enabled:
		{
			Graphics2D g23 = (Graphics2D)canvas2.getGraphics();	 // Getting the graphics for the second image
			g23.setPaint(bgcolor);	// Setting the paint to gray
			g23.fillRect(0, 0, iwidth, iheight);	// Filling in the entire first image with gray to overwrite the old data it contained
			g23.dispose();	// Telling Java that we are finished messing with the image's graphics
		}
		if (mouse)	// If the mouse is enabled:
		{
			mxrot += (MouseInfo.getPointerInfo().getLocation().x-mxa); 	// Add the distance the mouse has moved along the x axis to the camera rotation variable
			myrot += (MouseInfo.getPointerInfo().getLocation().y-mya);	// Add the distanve the mouse has moved along the y axis to the camera rotation variable
			r.mouseMove(mxa,mya);	// Move the mouse back to its anchor point so the user doesn't need to worry about running into the side of the screen of clicking on other windows
		}
		camx = 0;	// Making sure the camera's x-offset is 0
		if (d3)	// If 3D is enabled, however:
			camx = -50;	// Set the camera's x-offset to 50
		vertRay(canvas);	// Draw the world on the first image
		if (d3)	// If 3D is enabled, next:
		{
			camx = 50;	// Set the camera's x-offset to -50, and
			vertRay(canvas2);	// Draw the world on the second image
		}
		if (antialias)	// If antialiasing is enabled:
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);	// Tell Java to antialias the images
		g2.drawImage(Func.fisheye(canvas,-20), 0, 0, cwidth, cheight, null);	// Calculate any barrel-distortion, and draw the first image to the canvas
		if (d3)	// If 3D is enabled:
			g2.drawImage(Func.fisheye(canvas2,20), iwidth/2, 0, cwidth, cheight, null);	// Calculate any barrel-distortion, and draw the second image to the canvas as well
		try	// Tell java we will handle any errors that might be caused, in this case, by attempting to force java to call a method that may or may not even exist
		{
			if (dregs)	// If a method has requested to be allowed to draw on the canvas:
				dregm.invoke(this, (Object)g);	// Call the method that is specified as handling whatever drawing the external program wishes to do, sending it the graphics for the canvas so that is is able to draw on it
		}
		catch(Exception j)	// If we run into a problem while calling the method
		{
			j.printStackTrace();	// Print out the error's technical information
			System.err.println("Error while calling external draw method \"" + dregm.getName() + "\"");	// Let the user know that something went wrong
		}
		if (fps)	// If the FPS should be printed to the screen:
		{
			g2.setPaint(Color.WHITE);	// Set the paint's color to white, and
			g2.drawString((int)(1000f/(timer.getTime()-lastTime))+" FPS",0,10);	// Draw the FPS to the upper-left hand corner of the screen
			lastTime = timer.getTime();
		}
		//System.out.println(((SharedVector)qstor).crcalcs + " | " + qstor);
	}
	public void vertRay(BufferedImage i)	// Method that will figure out positions of the vertecies in the scene, and draw the image that the user will end up seeing, onto an image ('BufferedImage')
	{
		try
		{
			Triangle[] ctris = new Triangle[tris.size()];	// Set up an array to store the Triangles that will be modified during the rendering proccess
			ArrayList<Light> clights = new ArrayList<Light>();	// Set up an ArrayList to store the Lights that will be modified during the rendering proccess
			Vector first = new Vector(0, 0, 0), v = new Vector(0, 0, 0);	// Declare Vectors that will be used to while changing 3D coordinates to 2D coordinates
			Graphics2D g = (Graphics2D)i.createGraphics();	// Getting the graphics for the image that is being drawn on
			if (antialias)
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int xrots = myrot;	// Making a variable to store the value of the camera's x-rotation, as it may change while we are rendering the image
			int zrots = mxrot;	// Making a variable to store the value of the camera's z-rotation, as it may change while we are rendering the image
			int a = 0;	// Making a counter so we know where we are in the array holding the modified Triangles
			for (Triangle s : tris)
				for (Vector q : s.verts)
					if (q instanceof SharedVector)
					{
						((SharedVector)q).calculated = false;
						((SharedVector)q).rcalc = false;
					}
			for (Triangle z : tris)	// For every Triangle in the scene:
			{
				Triangle o = z.deepCopy();				// deepCopy takes WAAAAY too much time :/ [EDIT] Fixed! deepCopy was reloading files for some reason...
				for (Vector orig : o.verts)
				{
					if (!(orig instanceof SharedVector))
					{
					orig.osub(camPos);	// need osub instead of sub to modify the vector that is ACTUALLY being used
					orig.rotateZ(zrots/1000f, new Vector(0, 0, camz));
					orig.rotateX(xrots/1000f, new Vector(0, 0, camz));
					orig.osmul(iwidth*(1920f/iwidth));
					}
					else
					{
						SharedVector sv = (SharedVector)orig;
						if (!sv.calculated)
						{
						sv.crcalcs = new Vector(sv.x, sv.y, sv.z);
						sv.crcalcs.osub(camPos);	// need osub instead of sub to modify the vector that is ACTUALLY being used
						sv.crcalcs.rotateZ(zrots/1000f, new Vector(0, 0, camz));
						sv.crcalcs.rotateX(xrots/1000f, new Vector(0, 0, camz));
						//System.out.print(sv.crcalcs);
						sv.crcalcs.osmul(iwidth*(1920f/iwidth));
						//sv.crcalcs.osmul(iwidth); //???
						//System.out.println(" -> " + sv.crcalcs);
						sv.calculated = true;
						}
					}
				}
				ctris[a] = o;
				a++;
			}
			for (Light l : lights)
			{
				Light o = l.deepCopy();
				o.pos.osub(camPos);
				o.pos.rotateZ(zrots/1000f, new Vector(0, 0, camz));
				o.pos.rotateX(xrots/1000f, new Vector(0, 0, camz));
				o.pos.osmul(iwidth*(1920f/iwidth));
				clights.add(o);
			}
			Arrays.sort(ctris);
			for (Triangle o : ctris)
			{
				if (o.render)
				{
					int z = 0;
					Polygon poly = new Polygon();
					ArrayList<Vector> pts = new ArrayList<Vector>();
					boolean render = true;
					for (Vector orig : o.verts)
					{
						if (orig instanceof SharedVector)
						{
							SharedVector rsv = ((SharedVector)orig);
							if (!rsv.rcalc)
							{
								v = Func.calc(rsv.crcalcs);
								rsv.v=v.deepCopy();
								rsv.v.render = v.render;
								rsv.rcalc = true;
							}
							else
							{
								v = new Vector(rsv.v.x,rsv.v.y,rsv.v.z);
								v.render = rsv.v.render;
							}
						}
						else
							v = Func.calc(orig);
						if (d3)
						{
							v.x/=2;
							v.x += paper.iwidth/4;
							v.x/=2;
						}
						else
							v.x += paper.iwidth/2;
						v.y += paper.iheight/2;
						poly.addPoint((int)(v.x/psc/psc), (int)(v.y/psc/psc));
						pts.add(v);
						if (z==0)
							first = v;
						z++;
						if (!v.render)
							render = false;
					}
					if (render)
					{
						Color c = o.mat.diffuse;
						if (light)
						{
							double maxval = -255;
							for (Light l : clights)
							{
								Vector v1 = o.verts[0];
								if (v1 instanceof SharedVector)
									v1 = ((SharedVector)o.verts[0]).crcalcs;
								Vector v2 = o.verts[1];
								if (v2 instanceof SharedVector)
									v2 = ((SharedVector)o.verts[1]).crcalcs;
								Vector v3 = o.verts[2];
								if (v3 instanceof SharedVector)
									v3 = ((SharedVector)o.verts[2]).crcalcs;
								double val = (l.calc(v1)+l.calc(v2)+l.calc(v3))/3f*1000000000*100*l.intensity;
								if (val > maxval)
									maxval = val;
							}
							c = Func.tint(c, maxval);
						}
						g.setPaint(c);
						poly.addPoint((int)(first.x/psc/psc), (int)(first.y/psc/psc));
						g.fill(poly);
						g.setPaint(Color.BLACK);
						if (lines)
							g.draw(poly);
						if (texture && o.hastex)
							o.affineTexture(canvas, new Triangle(pts.get(0), pts.get(1), pts.get(2), new Material("", Color.BLACK)), poly);
					}
				}
			}
			g.setPaint(Color.WHITE);
			g.dispose();
		}
		catch (Exception e){}
	}
	public Group load(String path, int x, int y, int z, float scale)
	{
		Group g = new Group(new ArrayList<Triangle>());
		y = -y;
		try
		{
			ArrayList<Vector> verts = new ArrayList<Vector>();
			ArrayList<Triangle> otris = new ArrayList<Triangle>();
			File file = new File(path);
			BufferedReader sc = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String input = sc.readLine();
			Material use = new Material("Default", Color.GRAY);
			Material.read(path.split("\\Q.\\E")[0] + ".mtl");
			Vector.count = 0;
			int tct = 0;
			while (input != null)
			{
				if (input.split(" ")[0].equals("v"))
				{
					verts.add(SharedVector.smul(new Vector(Double.parseDouble(input.split(" ")[1])+x, -Double.parseDouble(input.split(" ")[2])+y, Double.parseDouble(input.split(" ")[3])+z), scale)); // comment out the "Shared" for non experimental version
					Vector.count--;
					//System.out.println("Vertex Declared\t"+Vector.count);
				}
				if (input.split(" ")[0].equals("f"))
				{
					tct++;
					//System.out.println("Making a face\t"+Vector.count);
					Vector point1 = verts.get(Integer.parseInt(input.split(" ")[1])-1);
					Vector point2 = verts.get(Integer.parseInt(input.split(" ")[2])-1);
					Vector point3 = verts.get(Integer.parseInt(input.split(" ")[3])-1);
					if (point1 instanceof SharedVector)
						((SharedVector)point1).count++;
					if (point2 instanceof SharedVector)
						((SharedVector)point2).count++;
					if (point3 instanceof SharedVector)
						((SharedVector)point3).count++;
					Triangle t = new Triangle(point1, point2, point3, use);
					Vector.count-=3;
					otris.add(t);
					tris.add(t);
					//System.out.println("Face delcared\t"+Vector.count);
				}
				if (input.split(" ")[0].equals("usemtl"))
				{
					use = Material.lookup(input.split(" ")[1]);
				}
				input = sc.readLine();
			}
			qstor = verts.get(0);
			g = new Group(otris);
			System.out.println(Vector.count + " Vectors made.");
			System.out.println(verts.size() + " Vectors declared.");
			System.out.println(tct + " Triangles created.\n");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		return g;
	}
	public void cmove(Vector v)
	{
		camPos.x += v.x*iwidth;
		camPos.y += v.y*iwidth;
		camPos.z += v.z*iwidth;
	}
	public void registerClickEvent(paper p, String method)
	{
		for (Method m : p.getClass().getDeclaredMethods())
		{
			if (m.getName().equalsIgnoreCase(method))
			{
				cregs = true;
				cregm = m;
				return;
			}
		}
		cregs = false;
	}
	public void registerDrawEvent(paper p, String method)
	{
		for (Method m : p.getClass().getDeclaredMethods())
		{
			if (m.getName().equalsIgnoreCase(method))
			{
				dregs = true;
				dregm = m;
				return;
			}
		}
		dregs = false;
	}
	public void mousePressed(MouseEvent e)
	{
		try
		{
		if (cregs)
			cregm.invoke(this, new Object[]{e.getX(),e.getY(),e.getButton()!=MouseEvent.BUTTON1});
		}
		catch(Exception j)
		{
		System.err.println("ERROR");
		}
	}
	public void mouseClicked(MouseEvent e){}
	public void mouseReleased(MouseEvent e){}
	public void mouseExited(MouseEvent e){}
	public void mouseEntered(MouseEvent e){}
}

class Animator implements Runnable
{
	JFrame frame;
	public Animator(JFrame frame)
	{
		this.frame = frame;
	}
	public void run()
	{
		frame.repaint();
	}
}

class Vector
{
	public double x = 0, y = 0, z = 0;
	public boolean render = true;
	static int count = 0;
	public Vector(int x, int y, int z)
	{
		this.x = x;
		this.y = -y;	//INCONSISTANT@@@
		this.z = z;
		count++;
	}
	public Vector(double x, double y, double z)
	{
		this.x = x;
		this.y = y;		//INCONSISTANT@@@
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
	// write methods to rotate-y (yaw), and rotate around an arbitrary axis (for physics calculations)
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

class Triangle implements Comparable<Triangle>
{
	double D;
	Vector[] verts = new Vector[3];
	Vector normal;
	Material mat;
	BufferedImage tex;
	boolean hastex = false;
	boolean render = true;
	static double max = 0;
	public Triangle(Vector point1, Vector point2, Vector point3, Material mat)
	{
		verts[0] = point1;
		verts[1] = point2;
		verts[2] = point3;
		normal = Vector.cross(Vector.sub(point2, point1), Vector.sub(point3, point1));
		D = Vector.dot(point1, normal);
		this.mat = mat;
	}
	public void recalc()
	{
		normal = Vector.cross(Vector.sub(verts[1], verts[0]), Vector.sub(verts[2], verts[0]));
		D = Vector.dot(verts[0], normal);
	}
	public double avz()
	{
		return (verts[0].z+verts[1].z+verts[2].z)/3f;
	}
	public Triangle deepCopy()
	{
		Triangle ret = new Triangle(verts[0].deepCopy(), verts[1].deepCopy(), verts[2].deepCopy(), mat);
		ret.tex = tex;
		ret.hastex = hastex;
		ret.render = render;
		return ret;
	}
	public int compareTo(Triangle t)
	{
		double own = avd();
		double other = t.avd();
		if (own > other)
			return -1;
		else if (own < other)
			return 1;
		else
			return 0;
	}
	public double avd()
	{
		Vector v1 = verts[0];
		if (verts[0] instanceof SharedVector)
			v1 = ((SharedVector)verts[0]).crcalcs;
		Vector v2 = verts[0];
		if (verts[1] instanceof SharedVector)
			v2 = ((SharedVector)verts[1]).crcalcs;
		Vector v3 = verts[0];
		if (verts[2] instanceof SharedVector)
			v3 = ((SharedVector)verts[2]).crcalcs;
		Vector v = Vector.smul(Vector.add(Vector.add(v1,v2),v3), 1/3f);
		double dist = Math.sqrt(Math.pow(v.x, 2)+Math.pow(v.y, 2)+Math.pow(v.z-paper.camz, 2));
		return dist;
	}
	public void texture(BufferedImage canvas, Triangle space)	// Triangle space must have vertecies in this order: [center, ychange, xchange] for unwarped image...
	{
		ArrayList<Point> points = space.pixels();
		double umodx = (space.verts[0].x - space.verts[2].x)/tex.getWidth();	// x scale
		double umody = (space.verts[0].y - space.verts[2].y)/tex.getHeight();	// x tilt
		double vmodx = (space.verts[0].x - space.verts[1].x)/tex.getWidth();	// y tilt
		double vmody = (space.verts[0].y - space.verts[1].y)/tex.getHeight();	// y scale
		for (Point p : points)
		{
			if (p.x > 0 && p.x < paper.iwidth && p.y > 0 && p.y < paper.iheight)
			{
				int color = 0;
				double x = space.verts[1].x-p.x;
				double y = space.verts[1].y-p.y;
				
				double a = umodx;
				double b = vmodx;
				double c = umody;
				double d = vmody;
				
				double v = ((a/c)*y-x)/((d*a/c)+b);
				double u = Math.abs((x-b*v)/a);
				v = Math.abs(v);
				
				if ((int)u <= 1)
					u += tex.getWidth()-1;
				if ((int)v <= 1)
					v += tex.getHeight()-1;
				if ((int)u >= tex.getWidth())
					u -= tex.getWidth();
				if ((int)v >= tex.getHeight())
					v -= tex.getHeight();
				if (0 > u || u > tex.getWidth())
					u = 1;
				if (0 > v  || v > tex.getHeight())
					v = 1;
				
				try
				{
					color = tex.getRGB((int)u, (int)v);
				}
				catch(Exception e)
				{
					System.err.println("ERROR GETTING <" + (int)u + ", " + (int)v + ">");
					System.exit(0);
				}
				canvas.setRGB((int)(p.x/paper.psc/paper.psc), (int)(p.y/paper.psc/paper.psc), color);
			}
		}
	}
	public void newTexture(BufferedImage canvas, Triangle space)	// Implementation will generate a simple UV map for tris, then just figure out where a ray intersects the triangle relative to its anchor coordinate (verts[0]), offset it by the UV lower bounds, then grab the color
	//													will most likely need to render at a rather low resolution for now, or at the least use calculations to estimate the colors for other areas (as in the barrel distortion, seeing as each calculation ~=1 vertex calculation
	{
		ArrayList<Point> pixels = space.pixels();	// pixel method should also be rewritten to be much faster....
		for (Point p : pixels)
		{
			//cast a ray through the pixel
			Vector ytt = Func.pointTri((int)p.x, (int)p.y, this); // NEEDS TO BE A THING!!!   makes a ray from cam to x, y and returns Vector representing intersection with the Triangle given
			//System.out.println(ytt);
			//rotate the a copy of the triangle *and the returned point* to be aligned to the z axis (has no depth)
//			Triangle copy = deepCopy().rotateX(/*value*/).rotateY(/*value*/).rotateZ(/*value*/);
			//use the x,y relative to the anchor to determine u,v
			int u = -(int)(ytt.x-space.verts[0].x);
			//System.out.println(ytt.x + " : " + space.verts[0].x);
			int v = -(int)(ytt.y-space.verts[0].y);
			if (u<0)
				u=0;
			if (v<0)
				v=0;
			//draw to the screen
			//System.out.println(u + ", " + v + " to " + (int)p.x + ", " + (int)p.y);
			try{
				canvas.setRGB((int)p.x, (int)p.y, tex.getRGB(u%255, v%255));	// also needs to be adjusted for psc, but then again, so does the rest of the function... and barell... :|
			} catch(Exception e) {}
		}
		
	}
	public void affineTexture(BufferedImage canvas, Triangle space, Polygon poly)
	{
		BufferedImage test = tex;
		Graphics2D g2 = (Graphics2D)canvas.getGraphics();
		double degrees = 0;
		if (Math.abs(space.verts[2].y-space.verts[1].y) != 0)
		{
			degrees = Math.atan(Math.abs(space.verts[2].x-space.verts[1].x)/Math.abs(space.verts[2].y-space.verts[1].y));
		}
		AffineTransform trans = new AffineTransform();
		trans.translate((int)space.verts[2].x, (int)space.verts[2].y);
		if (degrees < max)
		{
			trans.scale(1, -1);
		}
		trans.rotate(degrees);
		if (space.verts[2].y>space.verts[1].y&&space.verts[2].x<space.verts[1].x)
		{
			g2.setPaint(Color.GREEN);
			g2.fillRect(0,0,100,100);
			trans.scale(-1, -1);
		}
		if (space.verts[2].y<space.verts[1].y&&space.verts[2].x<space.verts[1].x)
		{
			g2.setPaint(Color.RED);
			g2.fillRect(0,0,100,100);
			trans.scale(1, -1);
		}
		if (space.verts[2].y<space.verts[1].y&&space.verts[2].x>space.verts[1].x)
		{
			g2.setPaint(Color.BLUE);
			g2.fillRect(0,0,100,100);
			trans.scale(-1, 1);
		}
		double scale = Math.sqrt(Math.pow(space.verts[2].x-space.verts[1].x, 2)+Math.pow(space.verts[2].y-space.verts[1].y, 2));
		trans.scale(scale/(float)test.getWidth(), scale/(float)test.getHeight());
		max = degrees;
		//g2.setClip(poly);
		Vector a = space.verts[2];
		Vector b = space.verts[1];
		for (int x = 0; x < test.getWidth(); x++)
		{
			//double scale = (((a.y-by)/(a.x-b.x))*x+ax)/test.getHeight();
		}
		g2.drawImage(test, trans, null);
	}
	public ArrayList<Point> pixels()
	{
		ArrayList<Point> points = new ArrayList<Point>();
		int lowx = paper.iwidth;
		int highx = 0;
		int lowy = paper.iheight;
		int highy = 0;
		for (Vector v : verts)
		{
			if (v.x < lowx)
				lowx = (int)v.x;
			if (v.x > highx)
				highx = (int)v.x;
			if (v.y < lowy)
				lowy = (int)v.y;
			if (v.y > highy)
				highy = (int)v.y;
		}
		if (highy > paper.iheight)
			highy = paper.iheight;
		if (highx > paper.iwidth)
			highx = paper.iwidth;
		if (lowx < 0)
			lowx = 0;
		if (lowy < 0)
			lowy = 0;
		for (int x = lowx; x < highx; x++)
		{
			for (int y = lowy; y < highy; y++)
			{
				Point p = new Point(x, y);
				if (Func.contains(this, p))
					points.add(p);
			}
		}
		return points;
	}
	public boolean intersects(Triangle p)
	{
		p.recalc();
		boolean ret = false;
		boolean sign = false;
		boolean first = true;
		for (int yy = 0; yy < 3; yy++)
		{
			double n = Vector.dot(Vector.cross(Vector.sub(p.verts[1], p.verts[0]), Vector.sub(p.verts[2], p.verts[0])), verts[yy]) - Vector.dot(Vector.cross(Vector.sub(p.verts[1], p.verts[0]), Vector.sub(p.verts[2], p.verts[0])), p.verts[0]);
			if (first)
			{
				first = false;
				if (n > 0)
					sign = true;
			}
			if (n < 0 && sign)
				ret = true;
			if (n > 0 && !sign)
				ret = true;
		}
		return ret;
	}
}

class Ray
{
	Vector origin;
	Vector direction;
	public Ray(Vector origin, Vector direction)
	{
		this.origin = origin;
		this.direction = direction;
	}
	public Vector calc(double t)
	{
		Vector v = Vector.add(Vector.smul(direction, t), origin);
		return v;
	}
	public Vector zcalc()
	{
		Vector ndir = direction.norm();
		Vector ret = Vector.add(origin, Vector.smul(ndir, -origin.z));
		ret.z = 0;
		return ret;
	}
}
class Material
{
	private static ArrayList<Material> list = new ArrayList<Material>();
	String name;
	Color diffuse;
	public Material(String name, Color diffuse)
	{
		this.name = name;
		this.diffuse = diffuse;
	}
	public static boolean read(String filename)
	{
		try
		{
			File file = new File(filename);
			if (!file.exists())
				return false;
			BufferedReader sc = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String input = sc.readLine();
			String name = "Corrupted Material.";
			while (input != null)
			{
				if (input.split(" ")[0].equals("newmtl"))
					name = input.split(" ")[1];
				if (input.split(" ")[0].equals("Kd"))
				{
					list.add(new Material(name, new Color(Float.parseFloat(input.split(" ")[1]), Float.parseFloat(input.split(" ")[2]), Float.parseFloat(input.split(" ")[3]))));
				}
				input = sc.readLine();
			}
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	public static Material lookup(String name)
	{
		for (Material m : list)
		{
			if (m.name.equals(name))
				return m;
		}
		return new Material("Default", Color.GRAY);
	}
}

class Light
{
	Vector pos;
	double intensity;
	public Light(Vector position, double intensity)
	{
		pos = position;
		this.intensity = intensity;
	}
	public Light(Vector position, double intensity, float scale)
	{
		pos = Vector.smul(position, scale);
		this.intensity = intensity;
	}
	public double calc(Vector obj)
	{
		double dist = Math.sqrt(Math.pow(pos.x-obj.x, 2)+Math.pow(pos.y-obj.y, 2)+Math.pow(pos.z-obj.z, 2));
		return intensity/dist;
	}
	public Light deepCopy()
	{
		return new Light(new Vector(pos.x, pos.y, pos.z), intensity);
	}
}

class Group
{
	ArrayList<Triangle> tris = new ArrayList<Triangle>();
	ArrayList<Group> physgroups = new ArrayList<Group>(); // legacy
	public Group(ArrayList<Triangle> tris)
	{
		this.tris = tris;
	}
	public Group(Group g)
	{
		for (Triangle t : g.tris)
			tris.add(t);
	}
	public Vector center()
	{
		double x = 0;
		double y = 0;
		double z = 0;
		int t = 0;
		for (Triangle o : tris)
		{
			for (Vector p : o.verts)
			{
				x += p.x;
				y += p.y;
				z += p.z;
				t++;
			}
		}
		return new Vector(x/t, y/t, z/t);
	}
	public void makeTexture(String path)
	{
		try
		{
			BufferedImage img = ImageIO.read(new File(path));
			for (Triangle tri : tris)
			{
				tri.hastex = true;
				tri.tex = img;
			}
		}
		catch (Exception e)
		{
			System.out.println("Error loading a texture :(");
		}
	}
	public void phystick()
	{
		boolean drop = true;
		for (Triangle o : tris)
		{
			int i = 0;
			for (Group g : physgroups)
			{
				for (Triangle f : g.tris)
				{
					if (o.intersects(f))
						drop = false;
				}
			}
		}
		if (drop)
			move(0, paper.iwidth*5, 0);
	}
	public void move(double mx, double my, double mz)
	{
		for (Triangle o : tris)
		{
			for (Vector p : o.verts)
			{
				int factor = 1;
				if (p instanceof SharedVector)
					factor = ((SharedVector)p).count;
				p.x += mx/factor;
				p.y += my/factor;
				p.z += mz/factor;
			}
		}
	}
	public boolean intersects(Group g)
	{
		double txl = -9999999,tyl=txl,tzl=txl,oxl=txl,oyl=txl,ozl=txl;
		double txs = 9999999,tys=txs,tzs=txs,oxs=txs,oys=txs,ozs=txs;
		for (Triangle t : tris)
		{
			for (Vector v : t.verts)
			{
				if (v.x > txl)
					txl=v.x;
				if (v.x < txs)
					txs=v.x;
				if (v.y > tyl)
					tyl=v.y;
				if (v.y < tys)
					tys=v.y;
				if (v.z > tzl)
					tzl=v.z;
				if (v.z < tzs)
					tzs=v.z;
			}
		}
		for (Triangle t : g.tris)
		{
			for (Vector v : t.verts)
			{
				if (v.x > oxl)
					oxl=v.x;
				if (v.x < oxs)
					oxs=v.x;
				if (v.y > oyl)
					oyl=v.y;
				if (v.y < oys)
					oys=v.y;
				if (v.z > ozl)
					ozl=v.z;
				if (v.z < ozs)
					ozs=v.z;
			}
		}
		if (oxl < txs || oyl < tys || ozl < tzs || txl < oxs || tyl < oys || tzl < ozs)
		{
			return false;
		}
		return true;
	}
	public void hide()
	{
		for (Triangle t : tris)
			t.render = false;
	}
	public void rotateY(Vector vec, double rots)
	{
		for (Triangle t : tris)
			for (Vector v : t.verts)
				v.rotateZ(rots, vec);
	}
}

class Func
{
	public static Vector calc(Vector v)
	{
		Ray r = new Ray(new Vector(paper.camx, 0, paper.camz), v);
		Vector ret = r.zcalc();
		if (v.z<=0.1)
			ret.render = false;
		return ret;
	}
	public static Color tint(Color c, double tint)
	{
		tint /= 50;
		if (c.getRed() >= c.getBlue() && c.getRed() >= c.getGreen())
		{
			if (tint > 255f/c.getRed())
			{
				tint = 255f/c.getRed();
			}
		}
		else if (c.getGreen() >= c.getBlue() && c.getGreen() >= c.getRed())
		{
			if (tint > 255f/c.getGreen())
			{
				tint = 255f/c.getGreen();
			}
		}
		else if (c.getBlue() >= c.getRed() && c.getBlue() >= c.getGreen())
		{
			if (tint > 255f/c.getBlue())
			{
				tint = 255f/c.getBlue();
			}
		}
		int red = (int)(c.getRed()*tint);
		if (red < 0)
			red = 0;
		int green = (int)(c.getGreen()*tint);
		if (green < 0)
			green = 0;
		int blue = (int)(c.getBlue()*tint);
		if (blue < 0)
			blue = 0;
		return new Color(red, green, blue);
	}
	public static boolean contains(Triangle t, Point p)
	{
		return ((p.x-t.verts[1].x)*(t.verts[0].y-t.verts[1].y)-(p.y - t.verts[1].y)*(t.verts[0].x-t.verts[1].x) <= 0) && ((p.x-t.verts[2].x)*(t.verts[1].y-t.verts[2].y)-(p.y - t.verts[2].y)*(t.verts[1].x-t.verts[2].x) <= 0) && ((p.x-t.verts[0].x)*(t.verts[2].y-t.verts[0].y)-(p.y - t.verts[0].y)*(t.verts[2].x-t.verts[0].x) <= 0);
	}
	public static BufferedImage fisheye(BufferedImage img, int offset)
	{
		if (!paper.rift)
			return img;
		BufferedImage ret = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		double correction = Math.sqrt(Math.pow(img.getWidth(),2) + Math.pow(img.getHeight(),2))/2.5;// /strength
		
		int xskip = 10;
		int yskip = 10;
		for (int x = 0; x < img.getWidth()-offset; x+=xskip)
		{
			for (int y = 0; y < img.getHeight(); y+=yskip)
			{
				int width = img.getWidth()/2;
				int height = img.getHeight()/2;
				int newx = x-width+offset;
				int newy = y-height;
				double theta;
				double a = Math.sqrt(newy*newy+newx*newx)/correction;
				if (a == 0)
					theta = 1;
				else
					theta = Math.tan(a)/a;
				int sX = (int)(width + theta*newx/1.2);
				int sY = (int)(height + theta*newy/1.2);
				if (sX-xskip>0&&sX+1<img.getWidth()&&sY-yskip-1>0&&sY+1<img.getHeight()&&y-yskip-1>0&&x-xskip>0)
				{
					try{
					if (paper.psc == 1)
						copySrcIntoDstAt(ret, img, xskip, yskip, x, y, sX, sY);
					else
						fill(ret, img, xskip, yskip, x, y, sX, sY);
					}
					catch (Exception e){e.printStackTrace();System.exit(0);}
				}
			}
		}
		return ret;
	}
	public static void fill(BufferedImage dest, BufferedImage src, int xscale, int yscale, int xStart, int yStart, int xSource, int ySource)
	{
		for (int x = 0; x < xscale; x++)
		{
			for (int y = 0; y < yscale; y++)
			{
				dest.setRGB(xStart-x, yStart-y, src.getRGB(xSource-x, ySource-y)); //33fps
			}
		}
	}
	

	static void copySrcIntoDstAt(final BufferedImage dst, final BufferedImage src, int xscale, int yscale, int xstart, int ystart, int xsource, int ysource) // taken from http://stackoverflow.com/questions/2825837/java-how-to-do-fast-copy-of-a-bufferedimages-pixels-unit-test-included <user: stacker>
	// WAAAY FASTER!!!
	{
		int[] srcbuf = ((DataBufferInt) src.getRaster().getDataBuffer()).getData();
		int[] dstbuf = ((DataBufferInt) dst.getRaster().getDataBuffer()).getData();
		int width = src.getWidth();
		int dstoffs = xstart/2 + ystart * width;
		int srcoffs = xsource/2 + ysource * width;
		for (int y = 0 ; y < yscale ; y++ , dstoffs-= width, srcoffs -= width ) 
		{
			System.arraycopy(srcbuf, srcoffs-1, dstbuf, dstoffs-1, xscale/2);
		}
	}
	
	//00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
	
	public static Vector pointTri(int sx, int sy, Triangle p)
	{
		Vector q = new Vector(paper.camx, 0, paper.camz);
		/*for (Vector i : p.verts)
		{
			i.rotateZ(paper.mxrot/1000f, new Vector(0, 0, paper.camz));
			i.rotateX(paper.myrot/1000f, new Vector(0, 0, paper.camz));
			//System.out.println(i);
		}*/
		Ray r = new Ray(q, new Vector((sx-paper.iwidth/2)/6400f, (sy-paper.iheight/2)/6400f, 0));
		//System.out.println("Cast ray from " + r.origin + " to " + r.direction);
		double t = 0;
		t = -( Vector.dot(p.normal, r.origin) + p.D  ) / ( Vector.dot(p.normal, r.direction) );
		//System.out.println("t:\t"+t);
		//Vector v = Vector.add(Vector.smul(r.direction, t), r.origin);
		//Vector a = Vector.sub(p.verts[1], p.verts[0]);
		//Vector b = Vector.sub(v, p.verts[0]);
		//Vector c = Vector.cross(a, b);
		//if (Vector.dot(c, p.normal) > 0)
		//{
			//System.out.println("Passed 1st test!");
			//a = Vector.sub(p.verts[2], p.verts[1]);
			//b = Vector.sub(v, p.verts[1]);
			//c = Vector.cross(a, b);
			//if (Vector.dot(c, p.normal) > 0)
			//{
				//System.out.println("Passed 2nd test!");
				//a = Vector.sub(p.verts[0], p.verts[2]);
				//b = Vector.sub(v, p.verts[2]);
				//c = Vector.cross(a, b);
				//if (Vector.dot(c, p.normal) > 0)
				//{
					//System.out.println("Passed all tests!");
					//System.exit(0);
					return Vector.smul(Vector.add(Vector.smul(r.direction, t), r.origin), 1f/1000);
				//}
			//}
		//}
		//else
		//System.out.println("Fail test1");
		//System.out.println("No intersection.");
		//return paper.origin;
	}
	
	//00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
	

}

class Timer implements Runnable
{
	private int x = 0;
	private Thread t;
	private volatile boolean stopped = false;
	public Timer()
	{
		t = new Thread(this);
		t.start();
	}
	public void run()
	{
		while(!stopped)
		{
			try
			{
				Thread.sleep(1);
				x++;
			}
			catch(Exception e)
			{
				System.out.println("ERROR!!!");
			}
		}
	}
	public void reset()
	{
		x = 0;
	}
	public int getTime()
	{
		return x;
	}
	public void release()
	{
		stopped = true;
		t = null;
	}
}
