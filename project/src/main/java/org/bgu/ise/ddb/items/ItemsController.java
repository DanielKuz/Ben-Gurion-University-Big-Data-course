/**
 * 
 */
package org.bgu.ise.ddb.items;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.http.HttpServletResponse;

import org.bgu.ise.ddb.MediaItems;
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
 * @author Alex
 *
 */
@RestController
@RequestMapping(value = "/items")
public class ItemsController extends ParentController {
	
	/**
	 * The function copy all the items(title and production year) from the Oracle table MediaItems to the System storage.
	 * The Oracle table and data should be used from the previous assignment
	 */
	@RequestMapping(value = "fill_media_items", method={RequestMethod.GET})
	public void fillMediaItems(HttpServletResponse response) throws UnknownHostException{
		Connection con=null;
		PreparedStatement ps=null;
		String ttl = null;
		Integer pYear = null;
		try{
            Class.forName("oracle.jdbc.driver.OracleDriver"); //registration of the driver
            con = DriverManager.getConnection("jdbc:oracle:thin:@ora1.ise.bgu.ac.il:1521/ORACLE","ifliandb","abcd"); //creating connection
            String readQuery = "SELECT * from MediaItems";
            ps = con.prepareStatement(readQuery); // compiling of the query in the DB
			ResultSet rs = ps.executeQuery();//executing query
			while(rs.next()){//iterating over result set and for each entry extracting title and production year
				ttl = rs.getString("TITLE"); 
				pYear = rs.getInt("PROD_YEAR");
				try { //insertion of the media item to mongodb
					MongoClient mClient = new MongoClient( "localhost" , 27017 ); //creating a connection
					DB db = mClient.getDB("BDDB"); //connecting to my database ****check if right
					DBCollection collection = db.getCollection("mediaItems"); //accessing collection within db
					BasicDBObject mItem = new BasicDBObject(); //creating a new media item instance
					BasicDBObject dupliQuery = new BasicDBObject(); //creating query object
					dupliQuery.put("title", ttl); //query to avoid inserting existing media item 
					DBCursor cur = collection.find(dupliQuery); //creating cursor
					if (cur.hasNext() == false) { //media item does not exist in the system so insert otherwise do nothing
						mItem.put("title", ttl); //filling media item fields
						mItem.put("prudction_year", pYear);
						collection.insert(mItem); //insertion
						}
					mClient.close();
				}
				catch (MongoException exc) {
					System.out.println("Mongo Error:" + exc);}				
			}
			rs.close();
			ps.close();
			con.close();
		HttpStatus status = HttpStatus.OK;
		response.setStatus(status.value());
	}
		catch(ClassNotFoundException e){
            e.printStackTrace();
        }
        catch(SQLException e){
            e.printStackTrace();
        } finally {
            try{
                if (ps != null) {
                    ps.close();
                }
            }
            catch (SQLException e){
                e.printStackTrace();
            }
            try {
                if (con != null) {
                    con.close();
                }
            }
            catch(SQLException e){
                e.printStackTrace();
            }
        }
	}
	/**
	 * The function copy all the items from the remote file,
	 * the remote file have the same structure as the films file from the previous assignment.
	 * You can assume that the address protocol is http
	 * @throws IOException 
	 */
	@RequestMapping(value = "fill_media_items_from_url", method={RequestMethod.GET})
	public void fillMediaItemsFromUrl(@RequestParam("url")    String urladdress,
			HttpServletResponse response) throws IOException{
		System.out.println(urladdress);
		URL url = new URL(urladdress); //creating url instance
		try {
		BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream())); //reading remote file
		String row ; //first line
		while((row = br.readLine()) != null){
			String[] entry = row.split(",");
			String ttl = entry[0];
			int  pYear = Integer.valueOf(entry[1]);
			try { //insertion of the media item to mongodb
				MongoClient mClient = new MongoClient( "localhost" , 27017 ); //creating a connection
				DB db = mClient.getDB("BDDB"); //connecting to my database 
				DBCollection collection = db.getCollection("mediaItems"); //accessing collection within db
				BasicDBObject mItem = new BasicDBObject(); //creating a new media item instance
				BasicDBObject dupliQuery = new BasicDBObject(); //creating query object
				dupliQuery.put("title", ttl); //query to avoid inserting existing media item 
				DBCursor cur = collection.find(dupliQuery); //creating cursor
				if (cur.hasNext() == false) { //media item does not exist in the system so insert otherwise do nothing
					mItem.put("title", ttl); //feeling media item fields
					mItem.put("prudction_year", pYear);
					collection.insert(mItem); //insertion
				}
				mClient.close(); //closing connection
			}
			catch (MongoException exc) {
				System.out.println("Mongo Error:" + exc);}		
		}
		br.close();
		}
	 catch (FileNotFoundException e) {
        e.printStackTrace();}
	 catch (IOException e) {
    	e.printStackTrace();} 	
		HttpStatus status = HttpStatus.OK;
		response.setStatus(status.value());
	}
	
	/**
	 * The function retrieves from the system storage N items,
	 * order is not important( any N items) 
	 * @param topN - how many items to retrieve
	 * @return
	 */
	@RequestMapping(value = "get_topn_items",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(MediaItems.class)
	public  MediaItems[] getTopNItems(@RequestParam("topn")    int topN) throws UnknownHostException{
		MediaItems item;
		Integer c;
		ArrayList<MediaItems> mi = new ArrayList<MediaItems>();
		try { //insertion of the media item to mongodb
			MongoClient mClient = new MongoClient( "localhost" , 27017 ); //creating a connection
			DB db = mClient.getDB("BDDB"); //connecting to my database 
			DBCollection collection = db.getCollection("mediaItems"); //accessing collection within db
			DBCursor cur = collection.find(); //creating a cursor to iterate result set
			c = cur.count();
			if (topN > c) {System.out.println("Only " + c +" media items in the system");}
			while(topN > 0 && cur.hasNext()) { //iterating over result set
				DBObject MediaItem = cur.next();  //retaining media item out of cursor
				String ttl = (String) MediaItem.get("title");  //retaining it's info
				int prodY = (int) MediaItem.get("prudction_year");
				item = new MediaItems(ttl,prodY); // creating media item object
				mi.add(item);
				topN--; //decreasing topN to control number of media items
			}
			mClient.close();
			}
		catch (MongoException exc) {
			System.out.println("Mongo Error:" + exc);}
//		MediaItems m = new MediaItems("Game of Thrones", 2011);
//		System.out.println(m);
//		return new MediaItems[]{m};
		return mi.toArray(new MediaItems[mi.size()]); 

	}
	
	//this method checks if a media item exists in the system or not
	public boolean isExistMovie( String title) {
		System.out.println(title);
		boolean result = false;
		try { //try except
		MongoClient mClient = new MongoClient( "localhost" , 27017 ); //creating a connection
		DB db = mClient.getDB("BDDB"); //connecting to my database 
		DBCollection collection = db.getCollection("mediaItems"); //accessing collection within db  
		BasicDBObject searchQuery = new BasicDBObject(); //creating query object
		searchQuery.put("title", title); //query
		DBCursor cur = collection.find(searchQuery); //creating a cursor to iterate result set
		while (cur.hasNext()) { 
			result = true; //if has next means the movie exists in the database so result should be true otherwise stays false
			cur.next();
		}
		mClient.close(); //closing connection
		}//handling exceptions
		catch (MongoException exc) {
			System.out.println("Mongo Error:" + exc);}
		return result;
		}
}
