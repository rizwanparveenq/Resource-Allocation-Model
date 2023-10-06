Tools Used
UPPAAL
Java (Util, IO, Random, XML)
UPPAAL-Java Wrapper API
******************************

This folder contains: UPPAAL xml files for our resource allocation model. The file name: Patient-Caregiver-Resources+Wheelchair.xml is the file that shows our extended model which combines Patient-Caregiver-Medical Instrument+Wheelchair allocation. 

How to run the java file?


Let $UPPAAL_HOME be the directory where UPPAAL is located
Follow the steps below to compile and execute the program
1.	Copy the PC_Sim.xml and PCSimulate.java files to the $UPPAAL_HOME/demo directory
2.	Open a command prompt in the $UPPAAL_HOME directory
3.	Open the PCSimulate.java file and set the value of the variable “CSVOutput” at the start of the class to the path where the output CSV file is to be created.
Example: If it is set to “D:\\data.csv”, then the output data.csv file in created in the D: directory
4.	Type “javac -cp lib/model.jar demo/PCSimulate.java” in the cmd to compile the Java program
5.	Type “java -cp demo;lib/model.jar;uppaal.jar PCSimulate demo/PC_Sim.xml” in the cmd to execute the compiled Java class
6.	Check the path that is set in point 3, to view the output CSV file having the required simulation output

******************************

NOTE:

 
1.	Whenever the PC_Sim.xml is opened in UPPAAL, it creates following snippet at top of the xml file. This line must be removed before executing the java file in cmd, else it will throw exception.

<!DOCTYPE nta PUBLIC '-//Uppaal Team//DTD Flat System 1.1//EN' 'http://www.it.uu.se/research/group/darts/uppaal/flat-1_2.dtd'>

2.	Every time the java file is executed, the generated data in the CSV file will be replaced with new simulation data. The old values will be replaced with new values.
3.	Please try to keep P, C, treatment values same as in .xml file of UPPAAL to get consistent results. 
4.	Uncomment “setDelayValues” function under “simulateModel” to get random treatment values for the patients. Otherwise, the java file will take the treatment values as defined in .xml file. The same case applies to “setThresholdValue”.

5. Please ignore the column with "W" values. This column was added to run the experiment with additional UPPAAL automata (no of wheelchairs). This may require in future if the UPPAAL model is extended with more components (i.e., wheelchairs or other assitive devices which is assiged to the patients along with caregivers).

5. Please see the "CSV files" folder which contains three .csv files with three different threshold values. The data in those csv files are obtained and aggregated after running the simulation multiple times for different values of P, C, D, etc. This is to provide better readabiliy to our thesis readers.

******************************
