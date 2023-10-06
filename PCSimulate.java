import com.uppaal.engine.CannotEvaluateException;
import com.uppaal.engine.Engine;
import com.uppaal.engine.EngineException;
import com.uppaal.engine.EngineStub;
import com.uppaal.engine.Problem;
import com.uppaal.engine.QueryFeedback;
import com.uppaal.engine.QueryResult;
import com.uppaal.model.io2.XMLWriter;
import com.uppaal.model.core2.Data2D;
import com.uppaal.model.core2.DataSet2D;
import com.uppaal.model.core2.Document;
import com.uppaal.model.core2.Edge;
import com.uppaal.model.core2.Location;
import com.uppaal.model.core2.Property;
import com.uppaal.model.core2.PrototypeDocument;
import com.uppaal.model.core2.Query;
import com.uppaal.model.core2.QueryData;
import com.uppaal.model.core2.Template;
import com.uppaal.model.system.SystemEdge;
import com.uppaal.model.system.SystemLocation;
import com.uppaal.model.system.symbolic.SymbolicState;
import com.uppaal.model.system.symbolic.SymbolicTransition;
import com.uppaal.model.system.symbolic.SymbolicTrace;
import com.uppaal.model.system.concrete.ConcreteTrace;
import com.uppaal.model.system.concrete.ConcreteState;
import com.uppaal.model.system.UppaalSystem;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.awt.geom.Point2D;
import java.util.Random;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class PCSimulate
{

    public static int[] simuP;
    public static int[] simuC;
    public static int P = 50;    //number of patients (requests)
     public static int D = 40;   // this value is for generating different treatment times as per number of patients
    public static String CSVOutput = "D:\\data.csv";
    public static int simuPairs = 10;
    public static int ROUNDS = 10;
    public static int THRESHOLD = 46; // treatment time cut-off


    /**
     * Valid kinds of labels on locations.
     */
    public enum LKind {
        name, init, urgent, committed, invariant, exponentialrate, comments
    };
    /**
     * Valid kinds of labels on edges.
     */
    public enum EKind {
        select, guard, synchronisation, assignment, comments
    };
    /**
     * Sets a label on a location.
     * @param l the location on which the label is going to be attached
     * @param kind a kind of the label
     * @param value the label value (either boolean or String)
     * @param x the x coordinate of the label
     * @param y the y coordinate of the label
     */
    public static void setLabel(Location l, LKind kind, Object value, int x, int y) {
        l.setProperty(kind.name(), value);
        Property p = l.getProperty(kind.name());
        p.setProperty("x", x);
        p.setProperty("y", y);
    }
    /**
     * Adds a location to a template.
     * @param t the template
     * @param name a name for the new location
     * @param x the x coordinate of the location
     * @param y the y coordinate of the location
     * @return the new location instance
     */
    public static Location addLocation(Template t, String name, String exprate,
									   int x, int y)
    {
        Location l = t.createLocation();
        t.insert(l, null);
        l.setProperty("x", x);
        l.setProperty("y", y);
		if (name != null)
			setLabel(l, LKind.name, name, x, y-28);
		if (exprate != null)
			setLabel(l, LKind.exponentialrate, exprate, x, y-28-12);
        return l;
    }
    /**
     * Sets a label on an edge.
     * @param e the edge
     * @param kind the kind of the label
     * @param value the content of the label
     * @param x the x coordinate of the label
     * @param y the y coordinate of the label
     */
    public static void setLabel(Edge e, EKind kind, String value, int x, int y) {
        e.setProperty(kind.name(), value);
        Property p = e.getProperty(kind.name());
        p.setProperty("x", x);
        p.setProperty("y", y);
    }
    /**
     * Adds an edge to the template
     * @param t the template where the edge belongs
     * @param source the source location
     * @param target the target location
     * @param guard guard expression
     * @param sync synchronization expression
     * @param update update expression
     * @return
     */
    public static Edge addEdge(Template t, Location source, Location target,
							   String guard, String sync, String update)
    {
        Edge e = t.createEdge();
        t.insert(e, null);
        e.setSource(source);
        e.setTarget(target);
        int x = (source.getX()+target.getX())/2;
        int y = (source.getY()+target.getY())/2;
        if (guard != null) {
            setLabel(e, EKind.guard, guard, x-15, y-28);
        }
        if (sync != null) {
            setLabel(e, EKind.synchronisation, sync, x-15, y-14);
        }
        if (update != null) {
            setLabel(e, EKind.assignment, update, x-15, y);
        }
        return e;
    }

    public static void print(UppaalSystem sys, SymbolicState s) {
        System.out.print("(");
        for (SystemLocation l: s.getLocations()) {
            System.out.print(l.getName()+", ");
        }
        int val[] = s.getVariableValues();
        for (int i=0; i<sys.getNoOfVariables(); i++) {
            System.out.print(sys.getVariableName(i)+"="+val[i]+", ");
        }
        List<String> constraints = new ArrayList<String>();
        s.getPolyhedron().getAllConstraints(constraints);
        for (String cs : constraints) {
            System.out.print(cs+", ");
        }
        System.out.println(")");
    }

    public static Document createSampleModel()
    {
		// create a new Uppaal model with default properties:
		Document doc = new Document(new PrototypeDocument());
		// add global variables:
		doc.setProperty("declaration", "int v;\n\nclock x,y,z;");
		// add a TA template:
		Template t = doc.createTemplate(); doc.insert(t, null);
		t.setProperty("name", "Experiment");
		// the template has initial location:
		Location l0 = addLocation(t, "L0", "1", 0, 0);
		l0.setProperty("init", true);
		// add another location to the right:
		Location l1 = addLocation(t, "L1", null, 150, 0);
		setLabel(l1, LKind.invariant, "x<=10", l1.getX()-7, l1.getY()+10);
		// add another location below to the right:
		Location l2 = addLocation(t, "L2", null, 150, 150);
		setLabel(l2, LKind.invariant, "y<=20", l2.getX()-7, l2.getY()+10);
		// add another location below:
		Location l3 = addLocation(t, "L3", "1", 0, 150);
		// add another location below:
		Location lf = addLocation(t, "Final", null, -150, 150);
		// create an edge L0->L1 with an update
		Edge e = addEdge(t, l0, l1, "v<2", null, "v=1,\nx=0");
		e.setProperty(EKind.comments.name(), "Execute L0->L1 with v=1");
		// create some more edges:
		addEdge(t, l1, l2, "x>=5", null, "v=2,\ny=0");
		addEdge(t, l2, l3, "y>=10", null, "v=3,\nz=0");
		addEdge(t, l3, l0, null, null, "v=4");
		addEdge(t, l3, lf, null, null, "v=5");
		// add system declaration:
		doc.setProperty("system",
						"Exp1=Experiment();\n"+
						"Exp2=Experiment();\n\n"+
						"system Exp1, Exp2;");
		return doc;
    }


    public static Document loadModel(String location) throws IOException
    {
		try {
			// try URL scheme (useful to fetch from Internet):
			return new PrototypeDocument().load(new URL(location));
		} catch (MalformedURLException ex) {
			// not URL, retry as it were a local filepath:
			return new PrototypeDocument().load(new URL("file", null, location));
		}
    }

    public static Engine connectToEngine() throws EngineException, IOException
    {
		String os = System.getProperty("os.name");
		String here = System.getProperty("user.dir");
		String path = null;
		if ("Linux".equals(os)) {
			path = here+"/bin-Linux/server";
		} else {
			path = here+"\\bin-Windows\\server.exe";
		}
		Engine engine = new Engine();
		engine.setServerPath(path);
		engine.setServerHost("localhost");
		engine.setConnectionMode(EngineStub.BOTH);
		engine.connect();
		return engine;
    }

    public static UppaalSystem compile(Engine engine, Document doc)
		throws EngineException, IOException
    {
		// compile the model into system:
		ArrayList<Problem> problems = new ArrayList<Problem>();
		UppaalSystem sys = engine.getSystem(doc, problems);
		if (!problems.isEmpty()) {
			boolean fatal = false;
			System.out.println("There are problems with the document:");
			for (Problem p : problems) {
				System.out.println(p.toString());
				if (!"warning".equals(p.getType())) { // ignore warnings
					fatal = true;
				}
			}
			if (fatal) {
				System.exit(1);
			}
		}
		return sys;
    }

    public static SymbolicTrace symbolicSimulation(Engine engine,
													UppaalSystem sys)
		throws EngineException, IOException, CannotEvaluateException
    {
		SymbolicTrace trace = new SymbolicTrace();
		// compute the initial state:
		SymbolicState state = engine.getInitialState(sys);
		// add the initial transition to the trace:
		trace.add(new SymbolicTransition(null, null, state));
		while (state != null) {
			print(sys, state);
			// compute the successors (including "deadlock"):
			ArrayList<SymbolicTransition> trans = engine.getTransitions(sys, state);
			// select a random transition:
			int n = (int)Math.floor(Math.random()*trans.size());
			SymbolicTransition tr = trans.get(n);
			// check the number of edges involved:
			if (tr.getSize()==0) {
				// no edges, something special (like "deadlock"):
				System.out.println(tr.getEdgeDescription());
			} else {
				// one or more edges involved, print them:
				for (SystemEdge e: tr.getEdges()) {
					System.out.print(e.getProcessName()+": "
									 + e.getEdge().getSource().getPropertyValue("name")
									 + " \u2192 "
									 + e.getEdge().getTarget().getPropertyValue("name")+", ");
				}
			}
			// jump to a successor state (null in case of deadlock):
			state = tr.getTarget();
			// if successfull, add the transition to the trace:
			if (state != null) trace.add(tr);
		}
        return trace;
    }

    public static void saveXTRFile(SymbolicTrace trace, String file)
		throws IOException
    {
		/* BNF for the XTR format just in case
		   (it may change, thus don't rely on it)
		   <XTRFomat>  := <state> ( <state> <transition> ".\n" )* ".\n"
		   <state>     := <locations> ".\n" <polyhedron> ".\n" <variables> ".\n"
		   <locations> := ( <locationId> "\n" )*
		   <polyhedron> := ( <constraint> ".\n" )*
		   <constraint> := <clockId> "\n" clockId "\n" bound "\n"
		   <variables> := ( <varValue> "\n" )*
		   <transition> := ( <processId> <edgeId> )* ".\n"
		*/
		FileWriter out = new FileWriter(file);
		Iterator<SymbolicTransition> it = trace.iterator();
		it.next().getTarget().writeXTRFormat(out);
		while (it.hasNext()) {
			it.next().writeXTRFormat(out);
		}
		out.write(".\n");
		out.close();
    }



    public static ArrayList<Integer> getDelayIndices (UppaalSystem sys, int D) {
        Integer cur = 0;
        ArrayList<Integer> delayIndex = new ArrayList<Integer>();

        for (int i=0 ; i<sys.getNoOfVariables() ; i++) {
            String curVarName = "delay[" + cur.toString() + "]";
            if (curVarName.equals(sys.getVariableName(i))) {
                delayIndex.add(i);
                cur++;
            }
        }

        return delayIndex;
    }


    public static int getThresholdIndex (UppaalSystem sys) {

        for (int i=0 ; i<sys.getNoOfVariables() ; i++) {
            if ("threshold".equals(sys.getVariableName(i))) {
                return i;
            }
        }

        System.out.println("Threshold variable not found!");
        return -1;
    }
 
    /**
     * the setDelayValues function generate random treatment values
     */

    public static void setDelayValues (SymbolicState state, ArrayList<Integer> delayIndex, int startRange, int endRange) {
        Random random = new Random();
        for (int i=0 ; i<delayIndex.size() ; i++) {
            int curIndex = delayIndex.get(i);
            int curRandom = random.nextInt(endRange - startRange) + startRange;
            state.getVariableValues()[curIndex] = curRandom;
        }
    }

    public static void setThresholdValue (SymbolicState state, int thresholdIndex) {
        state.getVariableValues()[thresholdIndex] = THRESHOLD;
    }

    

    public static String setPValue(String nodeContent, int P) {
        int startIndex = nodeContent.indexOf("int P = ") + 8;
        int endIndex = startIndex;

        while (endIndex<nodeContent.length() && nodeContent.charAt(endIndex)>='0' && nodeContent.charAt(endIndex)<='9') {
            endIndex++;
        }
        endIndex--;

        String ans = nodeContent.substring(0, startIndex) + Integer.toString(P) + nodeContent.substring(endIndex+1, nodeContent.length());
        return ans;
    }

    public static String setCValue(String nodeContent, int C) {
        int startIndex = nodeContent.indexOf("int C = ") + 8;
        int endIndex = startIndex;

        while (endIndex<nodeContent.length() && nodeContent.charAt(endIndex)>='0' && nodeContent.charAt(endIndex)<='9') {
            endIndex++;
        }
        endIndex--;

        String ans = nodeContent.substring(0, startIndex) + Integer.toString(C) + nodeContent.substring(endIndex+1, nodeContent.length());
        return ans;
    }



    public static void simulateModel (UppaalSystem sys, SymbolicState state, Engine engine, Query smcsim,
                                    ArrayList<Integer>delayIndex, int thresholdIndex,
                                    int delayRangeStart, int delayRangeEnd,
                                    FileWriter fw, int P, int C) 
    {
        try {
            // Comment the line below if the delay values should not be randomized
           // setDelayValues(state, delayIndex, delayRangeStart, delayRangeEnd);

            setThresholdValue(state, thresholdIndex);

            QueryResult res = engine.query(sys, state, options, smcsim, qf);

            StringBuilder sb = new StringBuilder();
            

            sb.append(P);
            sb.append(",");
            sb.append(C);
            sb.append(",");
            sb.append(D);
            sb.append(",");
            sb.append(state.getVariableValues()[thresholdIndex]);
            sb.append(",");

            for (int i=0 ; i<delayIndex.size() ; i++) {
                sb.append(state.getVariableValues()[delayIndex.get(i)]);
                sb.append(";");
            }
            sb.append(",");

            QueryData data = res.getData();

            double maxLenWQueueO = 0;
            double maxLenWQueueI = 0;
            double totalWaitingQueueO = 0;
            double totalWaitingQueueI = 0;

            double prevTime = 0;
            double prevValue = 0;

            for (String title: data.getDataTitles()) {
                DataSet2D plot = data.getData(title);
                int cur = 0;
                for (Data2D traj: plot) {
                    int idx = 0;
                    for (Point2D.Double p: traj) {
                        if (cur == 0) {
                            maxLenWQueueO = Math.max(maxLenWQueueO, p.y);
                            if (idx != 0) {
                                if (prevValue == p.y) {
                                    totalWaitingQueueO += (p.y * (p.x - prevTime));
                                }
                            }
                            prevValue = p.y;
                            prevTime = p.x;
                        }
                        else {
                            maxLenWQueueI = Math.max(maxLenWQueueI, p.y);
                            if (idx != 0) {
                                if (prevValue == p.y) {
                                    totalWaitingQueueI += (p.y * (p.x - prevTime));
                                }
                            }
                            prevValue = p.y;
                            prevTime = p.x;
                        }
                        idx++;
                    }
                    cur++;
                }
            }

            sb.append(maxLenWQueueO);
            sb.append(",");
            sb.append(maxLenWQueueI);
            sb.append(",");
            sb.append(totalWaitingQueueO);
            sb.append(",");
            sb.append(totalWaitingQueueI);
            sb.append(",");
            sb.append("\n");

            // Uncomment the lines below to generate the graph points in the XML file

            // for (String title: data.getDataTitles()) {
            //     DataSet2D plot = data.getData(title);
            //     for (Data2D traj: plot) {    
            //         for (Point2D.Double p: traj) {
            //             sb.append("(");
            //             sb.append(p.x);
            //             sb.append(":");
            //             sb.append(p.y);
            //             sb.append(");");
            //         }
            //         sb.append(",");
            //     }
            // }
            // sb.append("\n");

            fw.append(sb.toString());
        } catch (EngineException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        }

    }



    public static void simulateRounds (UppaalSystem sys, SymbolicState state, Engine engine, Query smcsim,
                                    ArrayList<Integer>delayIndex, int thresholdIndex,
                                    int delayRangeStart, int delayRangeEnd,
                                    int rounds, int P, int C) {

        try {
            File f = new File(CSVOutput);
            boolean fileExists = f.exists();
            FileWriter fw = new FileWriter(CSVOutput, true);

            if (!fileExists) {
                StringBuilder sb = new StringBuilder();
                sb.append("P,C,D,Threshold,Delay,maxLenQueueO,maxLenQueueI,totalWaitingQueueO,totalWaitingQueueI,PlotQueueO,PlotQueueI\n");
                fw.append(sb.toString());
            }

            for (int i=0 ; i<rounds ; i++) {
                simulateModel(sys, state, engine, smcsim, delayIndex, thresholdIndex,
                             delayRangeStart, delayRangeEnd, fw, P, C);
            }
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        }

    }



    public static void simulateWrapper(String fileName, int P, int C) {

        String xmlFilePath = fileName;
        try{
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            org.w3c.dom.Document document = documentBuilder.parse(xmlFilePath);
            Node declarationNode = document.getElementsByTagName("declaration").item(0);

            String declarationsString = declarationNode.getTextContent();

            String nodeContent = declarationNode.getTextContent();

            // Set the P and C values corresponding to simuP and simuC
            // in the model XML file
            nodeContent = setPValue(nodeContent, P);
            nodeContent = setCValue(nodeContent, C);
            declarationNode.setTextContent(nodeContent);


            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(new File(fileName));

            transformer.transform(domSource, streamResult);



            Document doc = null;
			if ("hardcoded".equals(xmlFilePath)) {
				// create a hardcoded model:
				doc = createSampleModel();
			} else {
				// load model from a file/internet:
				doc = loadModel(xmlFilePath);
			}

			// connect to the engine server:
			Engine engine = connectToEngine();

            // create a link to a local Uppaal process:
			UppaalSystem sys = compile(engine, doc);

			// Model-checking with customized initial state:
            SymbolicState state = engine.getInitialState(sys);

            ArrayList<Integer> delayIndex = getDelayIndices(sys, D);
            int thresholdIndex = getThresholdIndex(sys);

    /**
     * paste the simulate query which needs to be executed through this java file.
     */

            Query smcsim = new Query("simulate [<=200] { lenOfWQueueO, lenOfWQueueI }", "get simulation trajectories");
            // Query smcsim = new Query("simulate[<=300] {Patient(0).Wait_inDC, Patient(1).Wait_inDC }", "get simulation trajectories");


            simulateRounds(sys, state, engine, smcsim, delayIndex, thresholdIndex, 10, 100, ROUNDS, P, C);
			engine.disconnect();

        } catch (CannotEvaluateException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        } catch (EngineException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        } 
        catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } 
        catch (TransformerException tfe) {
            tfe.printStackTrace();
        } 
        catch (SAXException sae) {
            sae.printStackTrace();
        }

    }



    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {


        // Check if the arguments provided such as the filename are valid
        if (args.length<1) {
            System.out.println("Use one of the following arguments:");
            System.out.println("  hardcoded");
            System.out.println("  <URL>");
            System.out.println("  <path/file.xml>");
            System.exit(0);
        }


        // Sets the value of number of delays = number of Patients
        // Each delay value corresponds to the service/treatment time of the respective patient
        D = P;


        // Initialize the arrays containing the values of P and C
        simuP = new int[simuPairs];
        simuC = new int[simuPairs];


        // Setting the values of the P and C for simulation
        for (int i=0 ; i<simuPairs ; i++) {
            simuP[i] = P;
            simuC[i] = (i+1) * (P/10);  //multiple of patient numbers
        }

        //Setting the Threshold Parameter to 46
        THRESHOLD = 46;
        // Carrying out the simulation for all the "simuPairs" number of Pairs for threshold = 46
        for (int i=0 ; i<simuPairs ; i++) {
            simulateWrapper(args[0], simuP[i], simuC[i]);
        }

        // Setting the Threshold Parameter to 55
        THRESHOLD = 55;
        // Carrying out the simulation for all the "simuPairs" number of Pairs for threshold = 55
        for (int i=0 ; i<simuPairs ; i++) {
            simulateWrapper(args[0], simuP[i], simuC[i]);
        }

        // Setting the Threshold Parameter to 55
        THRESHOLD = 64;
        // Carrying out the simulation for all the "simuPairs" number of Pairs for threshold = 64
        for (int i=0 ; i<simuPairs ; i++) {
            simulateWrapper(args[0], simuP[i], simuC[i]);
        }	
           
    }

    public static final String options = "order 0\n"
		+ "reduction 1\n"
		+ "representation 0\n"
		+ "trace 0\n"
		+ "extrapolation 0\n"
		+ "hashsize 27\n"
		+ "reuse 1\n"
		+ "smcparametric 1\n"
		+ "modest 0\n"
		+ "statistical 0.01 0.01 0.05 0.05 0.05 0.9 1.1 0.0 0.0 1280.0 0.01";

    public static QueryFeedback qf =
		new QueryFeedback() {
			@Override
			public void setProgressAvail(boolean availability)
			{
			}

			@Override
			public void setProgress(int load, long vm, long rss, long cached, long avail, long swap, long swapfree, long user, long sys, long timestamp)
			{
			}

			@Override
			public void setSystemInfo(long vmsize, long physsize, long swapsize)
			{
			}

			@Override
			public void setLength(int length)
			{
			}

			@Override
			public void setCurrent(int pos)
			{
			}

			@Override
			public void setTrace(char result, String feedback,
								 SymbolicTrace trace, QueryResult queryVerificationResult)
			{
			}

			public void setTrace(char result, String feedback,
								 ConcreteTrace trace, QueryResult queryVerificationResult)
			{
			}
			@Override
			public void setFeedback(String feedback)
			{
				if (feedback != null && feedback.length() > 0) {
					System.out.println(feedback);
				}
			}

			@Override
			public void appendText(String s)
			{
				if (s != null && s.length() > 0) {
					System.out.println(s);
				}
			}

			@Override
			public void setResultText(String s)
			{
				if (s != null && s.length() > 0) {
					System.out.println(s);
				}
			}
		};
}
