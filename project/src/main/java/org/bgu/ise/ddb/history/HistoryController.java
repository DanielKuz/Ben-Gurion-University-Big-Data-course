/**
 * 
 */
package org.bgu.ise.ddb.history;
import java.io.IOException;
import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import org.bgu.ise.ddb.ParentController;
import org.bgu.ise.ddb.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.bgu.ise.ddb.registration.RegistarationController;
import org.bgu.ise.ddb.items.ItemsController;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

/**
 * @author Alex
 *
 */
@RestController
@RequestMapping(value = "/history")
public class HistoryController extends ParentController{
	
	/**
	 * The function inserts to the system storage triple(s)(username, title, timestamp). 
	 * The timestamp - in ms since 1970
	 * Advice: better to insert the history into two structures( tables) in order to extract it fast one with the key - username, another with the key - title
	 * @param username
	 * @param title
	 * @param response
	 */
	@RequestMapping(value = "insert_to_history", method={RequestMethod.GET})
	public void insertToHistory (@RequestParam("username")    String username,
			@RequestParam("title")   String title,
			HttpServletResponse response) throws IOException{
		System.out.println(username+" "+title);
		RegistarationController rgc = new RegistarationController(); //creating controllers instances to access their methods
		ItemsController ic= new ItemsController();
		try {
			if(rgc.isExistUser(username) == false) { //checking if given username is registered to the system if not ending func
				System.out.println("User doesn't exist in system please register");
				HttpStatus status = HttpStatus.CONFLICT;
				response.setStatus(status.value());	
				return;
			}
		}
		catch (IOException exc) {exc.printStackTrace();}
		if(ic.isExistMovie(title) == false) { ////checking if given umovie is registered to the system if not ending func
			System.out.println("Movie doesn't exist in system please insert it first");
			HttpStatus status = HttpStatus.CONFLICT;
			response.setStatus(status.value());	
			return;
		}
		try { //insertion of the media item to mongodb - both user and movie are in the system
			MongoClient mClient = new MongoClient( "localhost" , 27017 ); //creating a connection
			DB db = mClient.getDB("BDDB"); //connecting to my database 
			DBCollection log= db.getCollection("log"); //accessing collection within db
			BasicDBObject entry = new BasicDBObject(); //creating triple instance
			Long msts = System.currentTimeMillis();//generating time stamp in milliseconds
			entry.put("username", username); //feeling media item fields
			entry.put("MovieName", title);
			entry.put("TimeStamp", msts);
			log.insert(entry); //insertion
			mClient.close(); //closing connection
		}
		catch (MongoException exc) {
			System.out.println("Mongo Error:" + exc);}	
		HttpStatus status = HttpStatus.OK;
		response.setStatus(status.value());
	}

	
	
	
	/**
	 * The function retrieves users' history
	 * The function return array of pairs <title,viewtime> sorted by VIEWTIME in descending order
	 * @param username
	 * @return
	 */
	@RequestMapping(value = "get_history_by_users",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(HistoryPair.class)
	public  HistoryPair[] getHistoryByUser(@RequestParam("entity")    String username){
		ArrayList<HistoryPair> usersH = new ArrayList<HistoryPair>();
		try { 
			MongoClient mClient = new MongoClient( "localhost" , 27017 ); //creating a connection
			DB db = mClient.getDB("BDDB"); //connecting to my database 
			DBCollection collection = db.getCollection("log"); //accessing collection within db
			BasicDBObject searchQuery = new BasicDBObject(); //creating query object
			searchQuery.put("username", username); //query
			DBCursor cur = collection.find(searchQuery); //creating a cursor to iterate result set
			if (cur.hasNext() == false) {
				System.out.println("Given username is not in the system, please check if spelled correctly or register");
			}
			while (cur.hasNext()) { //iterating over result set
				DBObject user = cur.next(); //retaining user's info
				String ttl = (String) user.get("MovieName");
				Long ts= (Long) user.get("TimeStamp");
				Instant instant = Instant.ofEpochMilli( ts ); //retaining time stamp in to requested format
				HistoryPair hp = new HistoryPair(ttl,Date.from(instant)); //creating history pair instance
				usersH.add(hp); //adding it to the list
			}
			mClient.close(); //closing connection
		}
		//handling exceptions
		catch (MongoException exc) {
			System.out.println("Mongo Error:" + exc);}
//		HistoryPair hp = new HistoryPair("aa", new Date());
//		System.out.println("ByUser "+hp);
//		return new HistoryPair[]{hp};
		usersH.sort(Comparator.comparing(HistoryPair::getViewtime).reversed()); //sorting list in descending order via comperator on viewtime
		return usersH.toArray(new HistoryPair[usersH.size()]);
	}
	
	
	/**
	 * The function retrieves items' history
	 * The function return array of pairs <username,viewtime> sorted by VIEWTIME in descending order
	 * @param title
	 * @return
	 */
	@RequestMapping(value = "get_history_by_items",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(HistoryPair.class)
	public  HistoryPair[] getHistoryByItems(@RequestParam("entity")    String title){
		ArrayList<HistoryPair> itemsH = new ArrayList<HistoryPair>();
		try { //try except
			MongoClient mClient = new MongoClient( "localhost" , 27017 ); //creating a connection
			DB db = mClient.getDB("BDDB"); //connecting to my database 
			DBCollection collection = db.getCollection("log"); //accessing collection within db
			BasicDBObject searchQuery = new BasicDBObject(); //creating query object
			searchQuery.put("MovieName", title); //query
			DBCursor cur = collection.find(searchQuery); //creating a cursor to iterate result set
			if (cur.hasNext() == false) {
				System.out.println("Given movie title is not in the system, please check if spelled correctly or insert it");
			}
			while (cur.hasNext()) { //iterating over result set
				DBObject item = cur.next(); //retaining info from cursor
				String username = (String) item.get("username");
				Long ts= (Long) item.get("TimeStamp");
				Instant instant = Instant.ofEpochMilli( ts ); //requested time stamp format
				HistoryPair hp = new HistoryPair(username,Date.from(instant));// creating history pair instance
				itemsH.add(hp); //adding to list
			}
			mClient.close(); //closing connection
		}
		//handling exceptions
		catch (MongoException exc) {
			System.out.println("Mongo Error:" + exc);}

		itemsH.sort(Comparator.comparing(HistoryPair::getViewtime).reversed());//sorting list in descending order via comperator on viewtime

		return itemsH.toArray(new HistoryPair[itemsH.size()]);
		
//		HistoryPair hp = new HistoryPair("aa", new Date());
//		System.out.println("ByItem "+hp);
//		return new HistoryPair[]{hp};
	}
	
	/**
	 * The function retrieves all the users that have viewed the given item
	 * @param title
	 * @return
	 */
	@RequestMapping(value = "get_users_by_item",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(HistoryPair.class)
	public  User[] getUsersByItem(@RequestParam("title") String title){
		ArrayList<User> users = new ArrayList<User>();
		ArrayList<String> names=new ArrayList<String>();
		DBCursor u=null;
		String firstName=null;
		String lastName=null;
		try { 
			MongoClient mClient = new MongoClient( "localhost" , 27017 ); //creating a connection
			DB db = mClient.getDB("BDDB"); //connecting to my database ****check if right
			DBCollection collection = db.getCollection("log"); //accessing collection within db
			BasicDBObject searchQuery = new BasicDBObject(); //creating query object
			searchQuery.put("MovieName", title); //query
			DBCursor cur = collection.find(searchQuery); //creating a cursor to iterate result set
			if (cur.hasNext() == false) {
				System.out.println("Given movie title: " +title+" is not in the system, please check if spelled correctly or insert it");
			}
			while (cur.hasNext()) { //iterating on result set 
				DBObject user = cur.next(); //retaining username and adding to list
				String username = (String) user.get("username");
				names.add(username);
			}
			DBCollection collection2 = db.getCollection("users"); //accessing collection within db
			BasicDBObject searchQuery2= new BasicDBObject(); //creating query object
			for (int i=0; i < names.size();i++) {// iterating over retrieved usernames list for item
				searchQuery2.put("username",names.get(i));//creating query object
				u = collection2.find(searchQuery2);//query
				DBObject use = u.next(); //accessing the instance the cursor is pointing to
				firstName= (String) use.get("firstName"); //retrieving name and last name
				lastName=(String) use.get("FirstName");
				User uInstance = new User(names.get(i),firstName,lastName); //creating user instance
				users.add(uInstance); //inserting into the array
			}
			
			mClient.close(); //closing connection
		}
		//handling exceptions
		catch (MongoException exc) {
			System.out.println("Mongo Error:" + exc);}
		
		return users.toArray(new User[users.size()]);
		
//		User hp = new User("aa","aa","aa");
//		System.out.println(hp);
//		return new User[]{hp};
	}
	
	/**
	 * The function calculates the similarity score using Jaccard similarity function:
	 *  sim(i,j) = |U(i) intersection U(j)|/|U(i) union U(j)|,
	 *  where U(i) is the set of usernames which exist in the history of the item i.
	 * @param title1
	 * @param title2
	 * @return
	 */
	@RequestMapping(value = "get_items_similarity",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	public double  getItemsSimilarity(@RequestParam("title1") String title1,
			@RequestParam("title2") String title2){
		double ret = 0.0;
		User[] users1 = getUsersByItem(title1); //retaining all users who watched each movie
		User[] users2 = getUsersByItem(title2);
		ArrayList<String> u1 = new ArrayList<String>();
		ArrayList<String> u2 = new ArrayList<String>();
		for (User u : users1) {u1.add(u.getUsername());} //retaining username strings for each list
		for (User j : users2) {u2.add(j.getUsername());}
		Set<String> union = new HashSet<String>(u1); //creating a union set
		union.addAll(u2); //union calculation
		Set<String> inter = new HashSet<String>(u1); //creating an intersection set
		inter.retainAll(u2); //calculating intersection by retaining only strings that appear in both u1 and u2
		ret = (double)inter.size()/(double)union.size(); //calculating similarity
		return ret;
	}
	

}
