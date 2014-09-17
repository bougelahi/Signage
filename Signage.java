import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Signage {

	 //Declare variables for the slideshows
	boolean first = true;
	String schedule = ""; 
	//BufferedImage[] imgs; //Array for all the images to be played
	ArrayList<BufferedImage> imgs;
	ArrayList<String> imageNames; //Array to store all of the image names
	boolean newImages = true;
	boolean doneDownloading = false;
	boolean hasImages = false;
	SlideShowFrame frame; //Frame for displaying the images
	int currentImage = 0; //Index of imgs currently being displayed
	BufferedImage image; //Current Image
	private int interval;
	ServerSocket listenSock = null; //the listening server socket
	Socket sock = null;             //the socket that will actually be used for communication
	

	//These store the width and height of the screen connected to the Raspberry Pi
	//Used for resizing images
	static final int IMG_WIDTH = java.awt.Toolkit.getDefaultToolkit().getScreenSize().width;
	static final int IMG_HEIGHT = java.awt.Toolkit.getDefaultToolkit().getScreenSize().height;
	
	 private static final Properties properties = new Properties();
	
	 
	 
	 static {
	        try {
	        	String propFileName = "config.properties";
	            FileInputStream in = new FileInputStream(propFileName);
	            properties.load(in);
	            properties.getProperty("directory");
	            
	            
	           
	        } catch (IOException e) {
	            throw new ExceptionInInitializerError(e);
	        }
	    }
	
	 URL url;

		
	
	public Signage() {

		
		//imageUpdater calls the imageUpdate method
		//This downloads the schedule and images from the web server
		ActionListener imageUpdater = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				imageUpdate(false);
			}
		};//end imageUpdater
		
		
		try {
			url = new URL(properties.getProperty("fetch"));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		startSocket();
		imageNames = new ArrayList<String>();
		interval = Integer.parseInt(properties.getProperty("interval"));
		//imageUpdate is called every 1 minute
		Timer timer = new Timer(60000,imageUpdater);
		timer.setRepeats(true);
		timer.start();
		imageUpdate(false);
		
		//create the frame for the slideshow
		frame = new SlideShowFrame();

	}

	//imageUpdate is called by the ActionListener
	public void imageUpdate(boolean forced) {

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		String currentTime = sdf.format(new Date());
		int currentMinute = Integer.parseInt(currentTime.substring(currentTime.indexOf(":") + 1));
		if(currentMinute % interval == 0 || first || forced) {
			first = false;
			if(forced)
				System.out.println("FORCED UPDATE");
			
			
			try {
				//first attempt to download the schedule to txt file
				//System.out.println("Download Schedule!");
				downloadSchedule();
			}//end try
			catch(Exception e) {
				e.printStackTrace();
			}//end catch
			try {
				//Then download the images in the slideshow
			
					//System.out.println("Download Images!");
					downloadImages();
				
			}//end try
			catch(Exception e) {
				System.out.println("Exception while downloading images");
			}//end catch
			//get the array of images to be played at this moment
			
		}//end if
		else {

			System.out.println(currentTime + ": no update.");
		}//end else
	}//end imageUpdate

	
	//resizeImage changes the downloaded image's size to the screen's resolution automatically
	BufferedImage resizeImage(BufferedImage originalImage, int type) {
		
		BufferedImage resizedImage = new BufferedImage(IMG_WIDTH,IMG_HEIGHT,type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage,0,0,IMG_WIDTH,IMG_HEIGHT,null);
		
		return resizedImage; //returns the resized Image
	}//end resizeImage

	//downloadSchedule fetches the schedule from the server
	//and saves it to a text file
	public void downloadSchedule() {


		try {
			//First connect to the server
			
			HttpURLConnection con = (HttpURLConnection)(url.openConnection());
			int responseCode = con.getResponseCode();
			//System.out.println("Sending 'GET' request to URL : " + url);
			System.out.println("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
			newImages = false;
			hasImages = false;
			//Read in the text from the server one line at a time
			while((inputLine = in.readLine()) != null) {
				
				//response stores all of the text to save to the text file
				response.append(inputLine+"\r\n");
				String[] arr = inputLine.split(",");
				
				if(arr.length > 1 && arr[0].length() > 1) {
					hasImages = true;
					//store image names to download images from the server
					
					String temp = arr[0].substring(1);
					temp = temp.substring(temp.lastIndexOf("/")+1);
					if(!imageNames.contains(temp)){
					imageNames.add(temp);
					
				}
					
				}
			}//end while

			in.close();
			//Store the response in a single string
			schedule = response.toString();
		//	System.out.println(schedule);
			


		if(schedule.length() > 0) {	
				//BufferedWriter used to write text to a file
				BufferedWriter writer = null;
				writer = new BufferedWriter( new FileWriter("schedule.txt"));
				//Write the string to the file
				writer.write(schedule);

				if ( writer != null) {
					writer.close( );
				}//end if

		}
			
		

		}//end try 
		catch (MalformedURLException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}//end MalformedURLException catch
		catch (IOException e) {
			//Auto-generated catch block
			e.printStackTrace();
		}//end IOExcception catch
	}//end downloadSchedule


	public void downloadImages() {

		doneDownloading = false;

		//SwingWorker used to download each image concurrently
		SwingWorker<BufferedImage, Void> worker = new SwingWorker<BufferedImage, Void>() {
			@Override
			public BufferedImage doInBackground() {
				//Temporary array for storing the images
				BufferedImage temp_image = null;
				int type = 0;
				for(int i = 0; i < imageNames.size(); i++) {
					try {
						//Connect to the server using the image names in the URL to download them
						System.out.println("Writing image: " + imageNames.get(i));
						URL url2	= new URL(properties.getProperty("base") +imageNames.get(i));
						temp_image = ImageIO.read(url2);
						type = temp_image.getType() == 0? BufferedImage.TYPE_INT_ARGB : temp_image.getType();

						//Place resized Image inside of the temporary array
						temp_image = resizeImage(temp_image,type); //Resize
						if(imageNames.get(i).length() > 0 && imageNames.get(i).contains(".jpg")) {
							ImageIO.write(temp_image, "jpg", new File(imageNames.get(i))); 
						}//end if

					}//end try
					catch(Exception e) {
						e.printStackTrace();
					}//end catch
				}//end for
				return temp_image;
			}//end doInBackground

			@Override
			public void done() {

				//Called once all files have been downloaded, or at least attempted
				
				System.out.println("Writing done");
				doneDownloading = true;
				getCurrentContent();
				

			

			}//end done

		};//end worker
		worker.execute();
	}//end downloadImages


	//This method reads the "schedule.txt" file to get the list of currently
	//playing images in the slideshow throughout the day
	public void getCurrentContent() {
		ArrayList<BufferedImage> imageList = new ArrayList<BufferedImage>();
		imageNames = new ArrayList<String>();
		//ArrayList<String> names = new ArrayList<String>();
		try {
			//Open "schedule.txt" for reading
			BufferedReader br = new BufferedReader(new FileReader("schedule.txt"));
			String line;
			String[] attrs;

			//Go through the file line by line
			while ((line = br.readLine()) != null) {

				if(line.contains(",")) {

					//Separate the columns by commas
					attrs = line.split(",");

					if(attrs.length >= 5) {
						String imageName = attrs[0].substring(attrs[0].lastIndexOf("/")+1);
						imageNames.add(imageName);
						SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
						Calendar calendar = Calendar.getInstance();

						//Get actual day, time, etc.
						int currentDay = calendar.get(Calendar.DAY_OF_WEEK); 
						String currentTime = sdf.format(new Date());
						int currentHour = Integer.parseInt(currentTime.substring(0, currentTime.indexOf(":")));
						int currentMinute = Integer.parseInt(currentTime.substring(currentTime.indexOf(":") + 1));

						//Get the information from the file
						int day = Integer.parseInt(attrs[4+currentDay]);
						int startHour = Integer.parseInt(attrs[1]);
						int startMinute = Integer.parseInt(attrs[2]);
						int endHour = Integer.parseInt(attrs[3]);
						int endMinute = Integer.parseInt(attrs[4]);

						// (currentHour > startHour AND currentHour < endHour) OR
						// (currentHour = startHour AND currentMinute >= startMinute) OR
						// (currentHour = startHour AND currentHour = endHour AND currentMinute >= startMinute AND currentMinute <= endMinute)

						//Compare the actual day/time with the required days/times in the schedule
						//to determine what should play right now
						//System.out.println("Start time: " + startHour + ":" + startMinute);
						if(day == 1 && 
								((currentHour > startHour && currentHour < endHour) ||
								(currentHour == startHour && currentMinute >= startMinute) || 
								(currentHour == startHour && currentHour == endHour && currentMinute >= startMinute && currentMinute <= endMinute)||
								( currentHour == endHour && currentMinute <= endMinute))){

							System.out.println(imageName + " IS playing!");
							if(attrs[0].lastIndexOf("/") != -1){
								try{
								imageList.add(ImageIO.read(new File(imageName)));
							//	names.add(attrs[0].substring(attrs[0].lastIndexOf("/")+1));
								}
								catch(Exception e) {
									e.printStackTrace();
								}
							}//end if
						}//end outer if
						else {
							System.out.println(imageName + " IS NOT playing!");
						}//end else

					}//end if
				}//end wile
			}//end if
			br.close();
		
		}//end try

		catch(Exception e) {
			e.printStackTrace();
		}//end catch

		
		deleteOldImages();
		//Store the results and names in arrays
		imgs = imageList;
		System.out.println(imgs.size() + " images in slideshow");
	
	
	}//end getCurrentContent
	
	public void deleteOldImages() {
		File dir = new File(System.getProperty("user.dir"));
		File[] files = dir.listFiles(new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        return name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg") ;
		    }
		});
		
		for(File f : files) {
			if(!imageNames.contains(f.getName())) {
				try {
					if(f.delete()) {
						System.out.println(f.getName()  + " deleted successfully!");
					}
					else {
						System.out.println(f.getName()  + " deletion unccessful.");
					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	//This class contains the code for the Frame/Window of the program
	class SlideShowFrame extends JFrame {

		private static final long serialVersionUID = -8629357630980989062L;
		JPanel myPanel;
		public SlideShowFrame() {
			super();
			//remove all borders
			setUndecorated(true);
			setVisible(true);
			//set the window to full screen
			GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
			currentImage = 0;

			
			//Use displayer Thread to update what is shown on the screen
			final Displayer displayer = new Displayer();
			displayer.start();
			ActionListener slideshowChanger = new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
		
					displayer.changeImage();
				}//end actionPerformed
			};//end slideshowChanger

			//This timer sets the interval for changing pictures (10 seconds)
			Timer timer = new Timer(10000,slideshowChanger);
			timer.setRepeats(true);
			timer.start();
		}//end SlideShowFrame constructor

		//This class changes the picture being shown on the screen
		class Displayer extends Thread {

			public void run() {
			}//end run

			//changeImage changes which index of the imgs array to display next
			public void changeImage() {
				
			
				if(doneDownloading && imgs.size() > 0  ) {
					
					if(currentImage >= imgs.size()) {
						currentImage = 0;
					}//end if
					try {
						
						System.out.println("Slideshow image " + currentImage + ": " + imageNames.get(currentImage));//+ "  " + imageNames[currentImage]);
						setImage(imgs.get(currentImage));
					
						currentImage++;
				
					}//end try
					catch(Exception e) {
						e.printStackTrace();
					}//end catch
				}//end if

			}//end changeImage
		}//end Reader class	

		//this method sets the image variable to the new image to be displayed
		//then calls repaint to paint the screen
		public void setImage(BufferedImage newImage) {
			
				image = newImage;
				repaint();
			
			
		}//end setImage

		//This is called with the repaint method.
		//it simply displays the variable 'image' to the screen
		public void paint(Graphics g) {
			if(image != null) {
				g.drawImage(image,0,0,this);
			}//end if
			else {
				//System.out.println();
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);
			}
		}//end paint
	}//end SlideShowFrame class
	
	public void startSocket() {
		
		SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {
			@Override
			public Integer doInBackground() {
				try {
			  
			              listenSock = new ServerSocket(20222);
			 
			             while (true) {       //we want the server to run till the end of times
			            	 
			                 sock = listenSock.accept();             //will block until connection recieved
			          
			                 BufferedReader br =    new BufferedReader(new InputStreamReader(sock.getInputStream()));
			                 
			                 String line = "";
			                 String response = "";
			                 while ((line = br.readLine()) != null) {
			                     
			                     if(line.equals("ForceUpdate"))
			                    	 imageUpdate(true);
			                 }
			
			                 //Closing streams and the current socket (not the listening socket!)
			            
			                 br.close();
			             }
			         } catch (IOException ex) {
			             ex.printStackTrace();
			        }
				return 0;
			}
		
		@Override
		public void done() {

			//Called once all files have been downloaded, or at least attempted
		      
               try {
				sock.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Socket done");
			

		

		}//end done
		
			};
			worker.execute();
	}

	public static void main(String[] args) {
		Signage f = new Signage();

	}//end main
}//end Signage class
