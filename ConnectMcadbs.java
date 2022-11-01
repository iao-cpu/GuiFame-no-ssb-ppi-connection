// This class creates a  connection to a FAME database on Linux machine
// 26.10.2010
package no.ssb.ppi.connection;

import com.fame.timeiq.persistence.*;
import com.fame.timeiq.*;
import java.util.Properties;

public class ConnectMcadbs {	
	// Retrieve a time series from FAME database on Linux
	Server sr = Server.getInstance(); // create server instance
	Session ss = sr.getSession();  // create session
	Connection conn = null;		// create connection
	DataStore db1 = null;	// create datastore
	DataResourceSequence serach1 = new DataResourceSequence();
	String FAMEServer = "#48700@kpli-ovibos.ssb.no";
	Properties prop;	
	
	public void ConnectMcadbs() throws ObjectAccessChkException, DataStoreOpenChkException, ConnectionFailedChkException{	
		try{		
			//This property is for a wide area network.
			prop = new Properties();
			prop.put("speed", "slow");			
			try{
				conn = ss.createConnection(FAMEServer, "","", prop); // open connection
			}
			catch(ObjectAccessChkException e){
				e.printStackTrace();
			}
			catch(ConnectionFailedChkException e){
				System.out.print(e);
				return;
			}	
						
			/*
			 * One way to determine if the connection was made to an MCADBS server
			 */
			if(conn.supportsRemEval()){
				System.out.println(FAMEServer + " is an mcadbs");
			}
			else {
				if(FAMEServer == null || FAMEServer.length()== 0)
					System.out.println("A local connection is not an MCADBS");
				else 
					System.out.println(FAMEServer + " is not an MCADBS");
			}		
		} 
		catch (Throwable t) {
			t.printStackTrace();
		}	
		finally {
			if (conn != null)
				conn.close();
		}
	}
}