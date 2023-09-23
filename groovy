import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.File
import java.io.IOException
import com.boomi.execution.ExecutionUtil;
import groovy.xml.XmlUtil
import groovy.sql.Sql
import groovy.util.XmlSlurper
import java.nio.charset.StandardCharsets
import groovy.io.FileType


logger = ExecutionUtil.getBaseLogger();

StringBuilder stringBuilder = new StringBuilder();
String line;
String database=ExecutionUtil.getDynamicProcessProperty("IR_DATABASE");
String port =ExecutionUtil.getDynamicProcessProperty("IR_PORT");
String host=ExecutionUtil.getDynamicProcessProperty("IR_HOST");
def ir_user = ExecutionUtil.getDynamicProcessProperty("IR_USERID")
def ir_password = ExecutionUtil.getDynamicProcessProperty("IR_PASSWORD")
String ir_url="jdbc:postgresql://"+host+":"+port+"/"+database;
def sql = Sql.newInstance(ir_url, ir_user, ir_password)

String MAXIMO_WHERE_CONDITION=ExecutionUtil.getDynamicProcessProperty("MAXIMO_WHERE_CONDITION");

//String MAXIMO_WHERE_CONDITION="((CHANGEDATE BETWEEN ('2019-01-01 00:00:00') AND ('2021-11-01 00:00:00'))) AND (SITEID LIKE 'BEDFORD')";
int MAXIMO_RS_COUNT=100;
int MAXIMO_RS_START=0;
int MAXIMO_RS_TOTAL=0;
boolean KEEP_LOOPING=true;
String CHANGE_DATE_START=ExecutionUtil.getDynamicProcessProperty("CHANGE_DATE_START");
String CHANGE_DATE_END=ExecutionUtil.getDynamicProcessProperty("CHANGE_DATE_END");
String LANGUAGE=ExecutionUtil.getDynamicProcessProperty("LANGUAGE");
String MAXIMO_USERID=ExecutionUtil.getDynamicProcessProperty("MAXIMO_USERID");
String MAXIMO_PASSWORD=ExecutionUtil.getDynamicProcessProperty("MAXIMO_PASSWORD");
String MAXIMO_REST_URL=ExecutionUtil.getDynamicProcessProperty("MAXIMO_REST_URL");
String MAXIMO_REST_INTERFACE_NAME=ExecutionUtil.getDynamicProcessProperty("MAXIMO_REST_INTERFACE_NAME");
String IR_TALEND_OUTPUT=ExecutionUtil.getDynamicProcessProperty("IR_TALEND_OUTPUT");
String INTERFACE=ExecutionUtil.getDynamicProcessProperty("INTERFACE");
String CMMS_ID=ExecutionUtil.getDynamicProcessProperty("CMMS_ID");
String BATCH_NAME = ExecutionUtil.getDynamicProcessProperty("BATCH_NAME");
String folderPath=IR_TALEND_OUTPUT.replaceAll("/","\\") +"\\"+CMMS_ID+ "\\"+INTERFACE+"\\"+BATCH_NAME+"\\";
def xmlns="xmlns=\"http://www.ibm.com/maximo\""

def folder = new File(folderPath)
//Delete all the Existing files in IR_TALEND_OUTPUT FOLDER OF the corresponding Interface Batch
folder.eachFile { file ->
    file.delete()
}

//http://maximo76.meridium.com:9080/maxrest/rest/os/MIASSET?_lang=EN&_format=json&_compact=1&_maxItems=10&_rsStart=0&_uw=SITEID IN('BEDFORD')
// API endpoint
//def url = new URL("http://maximo76.meridium.com:9080/maxrest/rest/os/MIASSET")

def url = new URL(MAXIMO_REST_URL+MAXIMO_REST_INTERFACE_NAME)

logger.info("MAXIMO_REST_URL : "+MAXIMO_REST_URL);
logger.info("MAXIMO_REST_URL : "+MAXIMO_REST_URL);
logger.info("URL : "+url);
logger.info("LANGUAGE : "+LANGUAGE);
logger.info("MAXIMO_RS_COUNT : "+MAXIMO_RS_COUNT);
logger.info("MAXIMO_RS_START : "+MAXIMO_RS_START);
logger.info("CHANGE_DATE_START : "+CHANGE_DATE_START);
logger.info("CHANGE_DATE_END : "+CHANGE_DATE_END);
logger.info("MAXIMO_USERID : "+MAXIMO_USERID);
logger.info("CHANGE_DATE_END : "+CHANGE_DATE_END);
logger.info("MAXIMO_WHERE_CONDITION : "+MAXIMO_WHERE_CONDITION);



logger.info("----------VARIABLES DATA--------");
logger.info("BATCH NAME : ${BATCH_NAME}  " );
logger.info("CMMSID ;    ${CMMS_ID}  " );
logger.info("----------BEFORE RESULT--------");	    

def result = sql.rows("""
    SELECT distinct replace(filename,'.loadfile','') filename
    FROM autoextractor_control
    WHERE batch_name = '${BATCH_NAME}' AND active=1 AND cmms_id = '${CMMS_ID}'
""")

logger.info("result  : " +result);

	
result.each {

//while(MAXIMO_RS_START <=MAXIMO_RS_TOTAL)
while(KEEP_LOOPING)
{

logger.info("----------INSIDE EACH RESULT--------");	    
	 logger.info("filename  : " +it.filename);
	 
def filename=it.filename

filepath=folderPath+filename.replaceAll(".xml","")+"_"+MAXIMO_RS_START+".xml"

// Query parameters
def params = [
    _lang:LANGUAGE,
	_format:"xml",
	_compact:"1",
	_maxItems:MAXIMO_RS_COUNT.toString(),
	_rsStart:MAXIMO_RS_START.toString(),
	_CHANGEDATE:CHANGE_DATE_START,
	_CHANGEDATE:CHANGE_DATE_END,
	_uw: MAXIMO_WHERE_CONDITION
]


logger.info("MAXIMO_RS_COUNT : "+MAXIMO_RS_COUNT);
logger.info("MAXIMO_RS_START : "+MAXIMO_RS_START);

// Basic authentication credentials
def username = MAXIMO_USERID
def password = MAXIMO_PASSWORD

// Connection timeout
def timeout = 10000 // milliseconds

try {
    // Set basic authentication credentials
    def authString = "${username}:${password}"
    def authHeaderValue = "Basic " + authString.bytes.encodeBase64().toString()

    // Construct the URL with query parameters
    def queryString = params.collect { k, v -> "${URLEncoder.encode(k, 'UTF-8')}=${URLEncoder.encode(v, 'UTF-8')}" }.join('&')
    def queryURL = new URL(url.toString() + "?" + queryString)

    // Open a connection to the URL
    def connection = queryURL.openConnection() as HttpURLConnection

    // Set the connection timeout
    connection.connectTimeout = timeout

    // Set the request method and headers
    connection.requestMethod = "GET"
    connection.setRequestProperty("Authorization", authHeaderValue)

    // Make the GET request
    connection.connect()

    // Get the response code
    def responseCode = connection.responseCode

    // Ensure the response is successful
    if (responseCode == HttpURLConnection.HTTP_OK) {
        // Read the response
        def temp_response=connection.inputStream.text
        def repl_meridiumws=temp_response.replaceAll(xmlns,"")
        def response = repl_meridiumws.replaceAll(MAXIMO_REST_INTERFACE_NAME,"MIMERIDIUMWS")

        // Save the response to a file
        def file = new File(filepath)
        file.write(response)
		
				
		def encodedXml=filepath
		//def encodedXml = filepath.getBytes(StandardCharsets.UTF_8).toString("UTF-8")
//def encodedXml1 = encodedXml.replaceAll(/<\?xml.*\?>/,'') // Remove processing instruction


	
		def reader = new BufferedReader(new FileReader(file,StandardCharsets.UTF_8))	
		

            
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
            }
            
            String content = stringBuilder.toString();
			
//def replaced_xml = content.replaceAll(/<\?xml.*\?>/,'') // Remove processing instruction
//def replaced_xml = content// Remove processing instruction			
			
def parsedXml = new XmlParser().parseText(content)
MAXIMO_RS_TOTAL = Integer.parseInt(parsedXml.attributes().rsTotal)

logger.info("----------MAXIMO_RS_TOTAL--------"+MAXIMO_RS_TOTAL);


	
// Flush the StringBuilder and close the reader
stringBuilder.setLength(0); 
reader.close()

MAXIMO_RS_START=MAXIMO_RS_START+MAXIMO_RS_COUNT;

logger.info("----------After Increment MAXIMO_RS_START: --------"+MAXIMO_RS_START);

if(MAXIMO_RS_START > MAXIMO_RS_TOTAL)
{
KEEP_LOOPING=false
}
		
		logger.info("----------AFter Increment KEEP_LOOPING : --------"+KEEP_LOOPING);
		
    } else {
        println "Request failed. Response Code: $responseCode"
    }

logger.info("connection.responseCode : "+connection.responseCode);
logger.info("connection.inputStream.text: "+connection.inputStream.text);

    // Disconnect the connection
    connection.disconnect()
} catch (IOException e) {
    println "Error making the request: ${e.getMessage()}"
}
}
}
sql.close()
