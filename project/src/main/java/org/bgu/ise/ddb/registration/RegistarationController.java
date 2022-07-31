/**
 * 
 */
package org.bgu.ise.ddb.registration;
import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;


import javax.servlet.http.HttpServletResponse;
import org.bgu.ise.ddb.ParentController;
import org.bgu.ise.ddb.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
/**
 * @author Beni & Dani
 *
 */
@RestController
@RequestMapping(value = "/registration")
public class RegistarationController extends ParentController{
	/**
	 * The function checks if the username exist,
	 * in case of positive answer HttpStatus in HttpServletResponse should be set to HttpStatus.CONFLICT,
	 * else insert the user to the system and set to HttpStatus in HttpServletResponse HttpStatus.OK
	 * @param username
	 * @param password
	 * @param firstName
	 * @param lastName
	 * @param response
	 */
	@RequestMapping(value = "register_new_customer", method={RequestMethod.POST})
	public void registerNewUser(@RequestParam("username") String username,
			@RequestParam("password")    String password,
			@RequestParam("firstName")   String firstName,
			@RequestParam("lastName")  String lastName,
			HttpServletResponse response){
		System.out.println(username+" "+password+" "+lastName+" "+firstName);
		
		try {
			if (isExistUser(username) == true) // user exists 
			{ //as requested
				HttpStatus status = HttpStatus.CONFLICT;
				response.setStatus(status.value());
			}
			else { //inserting user because he is not exists in the system
				MongoClient mClient = new MongoClient( "localhost" , 27017 ); //creating a connection
				DB db = mClient.getDB("BDDB"); //connecting to my database ****check if right
				DBCollection collection = db.getCollection("users"); //accessing collection within db 
				BasicDBObject user = new BasicDBObject(); //generating 'user' instance
				user.put("username", username); //adding the info of the instance
				user.put("password", password); //*****think****
				user.put("firstName", firstName);
				user.put("lastName", lastName);
				Instant ts = Instant.now(); //generate registration time stamp
				user.put("registrationTimeStamp", Date.from(ts));// inserting to user obj
				collection.insert(user); //inserting user instance to collection
				mClient.close();//closing connection 
				HttpStatus status = HttpStatus.OK; //as requested
				response.setStatus(status.value());
			}
			}
		//handling exceptions
		catch (UnknownHostException exc) {
			System.out.println("Host Exception: " + exc);}
		catch (MongoException exc) {
			System.out.println("Mongo Error:" + exc);}
		catch (IOException exc) {
			System.out.println("IOException: " + exc);}		
	}
	/**
	 * The function returns true if the received username exist in the system otherwise false
	 * @param username
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value = "is_exist_user", method={RequestMethod.GET})
	public boolean isExistUser(@RequestParam("username") String username) throws IOException{
		System.out.println(username);
		boolean result = false;
		try { //try except
		MongoClient mClient = new MongoClient( "localhost" , 27017 ); //creating a connection
		DB db = mClient.getDB("BDDB"); //connecting to my database ****check if right
		DBCollection collection = db.getCollection("users"); //accessing collection within db  
		BasicDBObject searchQuery = new BasicDBObject(); //creating query object
		searchQuery.put("username", username); //query
		DBCursor cur = collection.find(searchQuery); //creating a cursor to iterate result set
		if (cur.hasNext()) { result = true;} //if has next means 'user name' exists in the database so result should be true otherwise stays false
		mClient.close(); //closing connection
		}//handling exceptions
		catch (MongoException exc) {
			System.out.println("Mongo Error:" + exc);}
		return result;
		}
	
	/**
	 * The function returns true if the received username and password match a system storage entry, otherwise false
	 * @param username
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value = "validate_user", method={RequestMethod.POST})
	public boolean validateUser(@RequestParam("username") String username,
			@RequestParam("password")    String password) throws IOException{
		System.out.println(username+" "+password);
		boolean result = false;
		try { //try except
			MongoClient mClient = new MongoClient( "localhost" , 27017 ); //creating a connection
			DB db = mClient.getDB("BDDB");//connecting to my database 
			DBCollection collection = db.getCollection("users"); //accessing collection within db  
			BasicDBObject searchQuery = new BasicDBObject(); //creating query object
			searchQuery.put("username", username);//query
			searchQuery.put("password", password);//query
			DBCursor cur = collection.find(searchQuery); //creating a cursor to iterate result set
			if (cur.hasNext()) { result = true;} //if has next means the received username and password match a system storage entry
			mClient.close(); //closing connection
		}
		catch (MongoException exc) {
			System.out.println("Mongo Error:" + exc);}
		
		return result;	
	}
	
	/**
	 * The function retrieves number of the registered users in the past n days
	 * @param days
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value = "get_number_of_registred_users", method={RequestMethod.GET})
	public int getNumberOfRegistredUsers(@RequestParam("days") int days) throws IOException{
		System.out.println(days+"");
		int result = 0;
		try { //try except
			MongoClient mClient = new MongoClient( "localhost" , 27017 ); //creating a connection
			DB db = mClient.getDB("BDDB"); //connecting to my database ****check if right
			DBCollection col = db.getCollection("users"); //accessing collection within db
			BasicDBObject dateQuery = new BasicDBObject(); //creating query object
			Instant currentTime = Instant.now(); //current time
			Date tstsf= Date.from(currentTime.minus(days, ChronoUnit.DAYS)); //time stamp to search from - subtracting  given number of days from current time
			dateQuery.put("registrationTimeStamp", new BasicDBObject("$gt", tstsf)); //gt operator for finding dates that are after the wanted date
			DBCursor cur = col.find(dateQuery);
			result = cur.count();//counting number of results
			mClient.close();//closing connection}
		}//handling exceptions
		catch (MongoException exc) {
			System.out.println("Mongo Error:" + exc);}
		return result;	
	}
	
	/**
	 * The function retrieves all the users
	 * @return
	 */
	@RequestMapping(value = "get_all_users",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(User.class)
	public  User[] getAllUsers() throws UnknownHostException{
		ArrayList<User> allUsers = new ArrayList<User>();
		try { //try except
			MongoClient mClient = new MongoClient( "localhost" , 27017 ); //creating a connection
			DB db = mClient.getDB("BDDB"); //connecting to my database ****check if right
			DBCollection collection = db.getCollection("users"); //accessing collection within db
			DBCursor cur = collection.find(); //creating a cursor to iterate result set
			while (cur.hasNext()) { 
				DBObject user = cur.next(); //retaining user's info
				String username = (String) user.get("username");
				String firstname = (String) user.get("firstName");
				String lastname = (String) user.get("lastName");
				User u = new User(username,firstname,lastname); //creating user instance
				allUsers.add(u);	 //adding to list
			}
			mClient.close(); //closing connection
		}
			//handling exceptions
			catch (MongoException exc) {
				System.out.println("Mongo Error:" + exc);}
			
			return allUsers.toArray(new User[allUsers.size()]); 
	}

}
