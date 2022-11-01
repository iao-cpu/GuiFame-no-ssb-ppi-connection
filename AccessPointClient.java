package no.ssb.ppi.connection;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import javax.swing.JOptionPane;

import com.fame.timeiq.Scalar;
import com.fame.timeiq.TiqObject;
import com.fame.timeiq.TiqType;

/**
 * AccessPointClient is the primary interface to the accessPoint Server.
 * This class manages the entire accessPoint session for the duration of the program.
 * 
 * AccessPointClient encapsulates the basic functionality of using a FAME accessPoint server
 * through the generic URL interface.  The result format is always an array
 * of serialized TiqObjects.
 */
public class AccessPointClient extends java.lang.Object{
	private String _baseUrl;
	private String _ticket;
	private long _lastRequestTime;
	private String _lastRequestString;
	private String _userid;
	private String _password;

	/**
	 *	Constructs the AccessPointClient with the base URL necessary to connect to the 
	 *  server. The baseUrl should include everything up to the first parameter.
	 *  That is, it should be possible to invoke:
	 *     baseUrl + "app=admin&cmd=get_info"
	 * 
	 * @param baseUrl string up to and including the question mark
	 */
	public AccessPointClient(String baseUrl){
		setBaseUrl(baseUrl);
	}

	/**
	 *	Sets the baseUrl.  Appends a question mark if it was missing.
	 *
	 *  @return string up to and including the question mark
	 */
	public void setBaseUrl(String baseUrl){
		_baseUrl = baseUrl;
		if (!_baseUrl.endsWith("?")){
			_baseUrl = _baseUrl + "?";
		}
	}

	/**
	 *	Gets the baseUrl that was set by the constructor or a setBaseUrl call.
	 *
	 *  @return string up to and including the question mark
	 */
	public String getBaseUrl(){
		return _baseUrl;
	}

	/**
	 *	If the user has logged in already, his ticket will be returned.  
	 *  Otherwise, ticket will be null.
	 *
	 *  @return user's ticket
	 */
	public String getTicket(){
		return _ticket;
	}

	/**
	 *  Logs in using user ID and password.
	 *
	 * @param userid user's login ID
	 * @param password user's password
	 */
	public synchronized void login(String userid, String password) throws Exception{
		TiqObject[] objs =
			issueRequest(
				"app=authenticate&cmd=login&userid=" + userid + "&password=" + password,
				false);
		_userid = userid;
		_password = password;
		Scalar loginRtn = (Scalar) objs[0];
		String name = loginRtn.getName();
		_ticket = loginRtn.getTiqValue().getStringValue();
		return;
	}
	public void logout() throws Exception{
		logout(_userid, _password);
	}

	/**
	 *	Logs the user out and sets the ticket to null.  The userid and password 
	 *  parameters are provided so that a user can force logout of an existing
	 *  session started on a different system.
	 *
	 * @param userid user's login id--optional and used only if the ticket is unknown
	 * @param password user's password--optional and used only if the ticket is unknown.
	 */
	public synchronized void logout(String userid, String password) throws Exception{
		if (_ticket != null){
			issueRequest("app=authenticate&cmd=logout", false);
		} 
		else{
			issueRequest(
				"app=authenticate&cmd=logout&userid=" + userid + "&password=" + password,
				false);
		}
		_ticket = null;
		return;
	}

	/**
	 * Issues this request to the server and returns whatever is produced.  Note that 
	 * all requests are issued as outformat "serialized".
	 *
	 * @param cmd full command string (such as "app=admin&cmd=get_products")
	 */
	public InputStream getStream(String cmd, boolean gzip)
		throws java.net.MalformedURLException, java.io.IOException{
		StringBuffer sb = new StringBuffer(cmd.length() + 50);
		sb.append(_baseUrl);
		sb.append(cmd);
		if (_ticket != null){
			sb.append("&ticket=");
			sb.append(_ticket);
		}
		if (gzip){
			sb.append("&gzip=true");
		}
		_lastRequestString = sb.toString();
		URL url = new URL(_lastRequestString);

		return url.openStream();
	}

	/**
	 * Issues this request to the server and returns whatever is produced.  Note that 
	 * all requests are issued as outformat "serialized".
	 *
	 * @param cmd full command string (such as "app=admin&cmd=get_products")
	 */
	public synchronized TiqObject[] issueRequest(String cmd, boolean gzip)
		throws
			APChkException,
			java.io.OptionalDataException,
			java.io.StreamCorruptedException,
			java.net.MalformedURLException,
			java.io.IOException{
		//request serialized TiqObject array
		cmd = cmd + "&outformat=timeiq";
	
		long start = System.currentTimeMillis();
		InputStream is = getStream(cmd, gzip);

		ObjectInputStream ois = null;
		if (!gzip){
			ois = new ObjectInputStream(is);
		} 
		else{
			GZIPInputStream gzis = new GZIPInputStream(is);
			ois = new ObjectInputStream(gzis);
		}

		TiqObject[] rtnObjects = null;
		try{
			rtnObjects = (TiqObject[]) ois.readObject();
		} 
		catch (java.lang.ClassNotFoundException e){
			throw new APChkException(-1, "TimeIQ classes not found.");
		}

		long end = System.currentTimeMillis();
		_lastRequestTime = end - start;

		// If the server generated an error, there will be two objects called 
		// ERROR_CODE and ERROR_MESSAGE
		if (rtnObjects[0].getName() != null
			&& rtnObjects.length == 2
			&& rtnObjects[0].getName().equals("ERROR_CODE")){
			int errCode = rtnObjects[0].getTiqValue().getIntValue();
			String errMsg = rtnObjects[1].getTiqValue().getStringValue();
			throw new APChkException(errCode, errMsg);
		}
		
		for (int ObjectIndex = 0; ObjectIndex < rtnObjects.length; ObjectIndex++){
			try{
				JOptionPane.showMessageDialog(null, "Object " + ((TiqObject)rtnObjects[ObjectIndex]).getName() + " contains " + rtnObjects[ObjectIndex].getObservations().size() + " observations");
			}
			catch(Exception e_ex){
				JOptionPane.showMessageDialog(null, e_ex);		
			}
		}
		return rtnObjects;
	}

	/**
	 *	Gets the number of milliseconds it took to complete the previous request.
	 *
	 *  @return milliseconds for last request
	 */
	public long getLastRequestTime(){
		return _lastRequestTime;
	}

	/**
	 *  Gets the last complete URL issued to the server.  This is useful for 
	 *  debugging or reporting after an exception was received.
	 *
	 *  @return last complete url (including baseUrl)
	 */
	public String getLastRequestString(){
		return _lastRequestString;
	}

	/**
	 *  accessPoint can return a special TiqObject indicating that there was a problem 
	 *  with an individual data object, or a data object was not found. This 
	 *  method checks for that particular TiqObject.
	 *
	 * @param tiqObject TiqObject returned from issueRequest()
	 * @return true if the object is a special missing/error filler
	 */
	public static boolean isNID(TiqObject tiqObject){
		if (tiqObject == null
			|| (!tiqObject.getTiqClass().isSeries()
				&& tiqObject.getTiqType() == TiqType.INTEGER)){
			String name = tiqObject.getName();
			if (name != null){
				if (name.toUpperCase().equals("NID")){
					return true;
				}
				if (name.toUpperCase().startsWith("NID_")){
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @return
	 */
	public String getPassword(){
		return _password;
	}

	/**
	 * @return
	 */
	public String getUserId(){
		return _userid;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable{
		logout();
		super.finalize();
	}
}
