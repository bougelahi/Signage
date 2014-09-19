import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
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

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingWorker;
import javax.swing.Timer;

public class Signage {

	 //Declare variables for the slideshows
	private boolean first = true;
	private String schedule = ""; 
	private ArrayList<BufferedImage> imgs;
	private ArrayList<String> imageNames; //Array to store all of the image names
	private ArrayList<String> playingImageNames;
	private boolean doneDownloading = false;
	private SlideShowFrame frame; //Frame for displaying the images
	private int currentImageIndex = 0; //Index of imgs currently being displayed
	private BufferedImage currentImage; //Current Image
	private int interval;
	private ServerSocket listenSock = null; //the listening server socket
	private Socket sock = null;             //the socket that will actually be used for communication
	private URL url; //URL to the server

	//These store the width and height of the screen connected to the Raspberry Pi
	private static final int IMG_WIDTH = java.awt.Toolkit.getDefaultToolkit().getScreenSize().width;
	private static final int IMG_HEIGHT = java.awt.Toolkit.getDefaultToolkit().getScreenSize().height;
	private final Properties properties;//Properties object to retrieve info from config file
	
	 public static void main(String[] args) {
			Signage f = new Signage();
			f.imageUpdate(false);

		}//end main
		
	
	
	public Signage() {
		
		//Retrieve properties from Config file
		properties = new Properties(); 
		try {
	        	String propFileName = "config.properties"; //File where properties are stored
	            FileInputStream in = new FileInputStream(propFileName);
	            properties.load(in);
	            properties.getProperty("directory");

	        } catch (IOException e) {
	            throw new ExceptionInInitializerError(e);
	        }
	    

		//imageUpdater calls the imageUpdate method every 60 seconds
		ActionListener imageUpdater = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				imageUpdate(false);
			}
		};//end imageUpdater
				
		try {
			url = new URL(properties.getProperty("fetch"));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		//Start the socket that waits for a Forced Update
		startSocket();
		
		//ArrayLists for storing Images and their names
		imgs = new ArrayList<BufferedImage>();
		imageNames = new ArrayList<String>();
		playingImageNames = new ArrayList<String>();
		
		//How many minutes between updates
		interval = Integer.parseInt(properties.getProperty("interval"));
		//imageUpdate is called every 1 minute
		Timer timer = new Timer(60000,imageUpdater);
		timer.setRepeats(true);
		timer.start();
		
		//create the frame for the slideshow
		frame = new SlideShowFrame();
		frame.setVisible(true);
	}//end Signage constructor
	
	

	//imageUpdate is called by the ActionListener
	public void imageUpdate(boolean forced) {

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		String currentTime = sdf.format(new Date());
		int currentMinute = Integer.parseInt(currentTime.substring(currentTime.indexOf(":") + 1));
		
		
		//Check whether it is the correct time to update 
		if(currentMinute % interval == 0 || first || forced) {
			first = false;
			if(forced){
				System.out.println("FORCED UPDATE");
			}
			
				//attempt to download schedule to local file, then download all of the images on the schedule
				downloadSchedule();
				downloadImages();
				
		}
		else {
			System.out.println(currentTime + ": no update.");
		}
	}//end imageUpdate

	//resizeImage changes the downloaded image's size to the screen's resolution automatically
	BufferedImage resizeImage(BufferedImage originalImage, int type) {
		
		BufferedImage resizedImage = new BufferedImage(IMG_WIDTH,IMG_HEIGHT,type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage,0,0,IMG_WIDTH,IMG_HEIGHT,null);	
		return resizedImage; //returns the resized Image
	}//end resizeImage

	//downloadSchedule fetches the schedule from the server, then saves it to a file.
	public void downloadSchedule() {

		try {
			
			imageNames.clear();
			
			//Connect to the server
			HttpURLConnection con = (HttpURLConnection)(url.openConnection());
			int responseCode = con.getResponseCode();
			System.out.println("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
	
			
			//Read in the text from the server one line at a time
			while((inputLine = in.readLine()) != null) {
				
				//append the line with new line characters to the response
				response.append(inputLine+"\r\n");
				
				//Split string by commas to get attributes as separate Strings
				String[] arr = inputLine.split(",");
				
				if(arr.length > 1 && arr[0].length() > 1) {
					//store image names to download images from the server
					String temp = arr[0].substring(1);
					temp = temp.substring(temp.lastIndexOf("/")+1);
					imageNames.add(temp);
				}
			}//end while

			in.close();
			//Store the response in a single string
			schedule = response.toString();
		
		if(schedule.length() > 0) {	
				//BufferedWriter used to write text to a file
				BufferedWriter writer = null;
				writer = new BufferedWriter( new FileWriter("schedule.txt"));
				//Write the string to the file
				writer.write(schedule);
				System.out.println("Schedule Written");

				if ( writer != null) {
					writer.close( );
				}
		}

		}//end try 
		catch (Exception e) {
			e.printStackTrace();
		}	
	}//end downloadSchedule


	public void downloadImages() {

		doneDownloading = false;

		//SwingWorker used to download each image concurrently
		SwingWorker<BufferedImage, Void> worker = new SwingWorker<BufferedImage, Void>() {
			@Override
			public BufferedImage doInBackground() {
				//Temporary image for resizing and writing
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


		try {
			//Open "schedule.txt" for reading
			BufferedReader br = new BufferedReader(new FileReader("schedule.txt"));
			String line;
			

			imgs.clear();
			playingImageNames.clear();
			imageNames.clear();
			
			//Go through the file line by line
			while ((line = br.readLine()) != null) {

				if(line.contains(",")) {

					//Separate the columns by commas
					String[] attrs = line.split(",");

					if(attrs.length >= 5) {
						String imageName = attrs[0].substring(attrs[0].lastIndexOf("/")+1);
						//All image names are added to imageNames to check which pictures to delete.
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
												
						
						//Format hour and minutes to 12 hour clock
						String hour = "" + startHour;
						String minute = "" + startMinute;
						if(startHour < 10) {
							hour = "0" + startHour;
						}
						else if(startHour > 12) {
							if(startHour-12 < 10) {
								hour = "0" + (startHour-12);
							}
							else{
								hour = "" + (startHour-12);
							}
						}
						
						if(startMinute < 10) {
							minute = "0" + startMinute;
						}
				
						//Compare the actual day/time with the required days/times in the schedule to determine what should be playing
						if(day == 1 && 
								(currentHour > startHour && currentHour < endHour) ||
								(currentHour == startHour &&currentHour < endHour && currentMinute >= startMinute) || 
								(currentHour == startHour && currentHour == endHour && currentMinute >= startMinute && currentMinute <= endMinute)||
								( currentHour == endHour && currentMinute <= endMinute)){
							
								//Add bufferedImage and imageName to their respective ArrayLists
								System.out.println(imageName + "("+hour+":"+minute+") IS playing!");
								imgs.add(ImageIO.read(new File(imageName)));
								//playingImageNames is strictly for images that are in a slideshow playing NOW
								playingImageNames.add(imageName);
						}
						else {							
							System.out.println(imageName + "("+hour+":"+minute+") IS NOT playing!");
						}

					}//end if
				}//end wile
			}//end if
			br.close();
		
		}//end try
		catch(Exception e) {
			e.printStackTrace();
		}
		
		System.out.println(imgs.size() + " images in slideshow");
		deleteOldImages();
	}//end getCurrentContent
	
	public void deleteOldImages() {
		File dir = new File(System.getProperty("user.dir"));
		File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				//Find only files with extension .jpg or .jpeg
		        return name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg") ;
		    }
		});
		
		
		//Go through all files in the directory and delete any JPEG images not on the schedule
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
		public SlideShowFrame() {
			super();
			//remove all borders and make window Full Screen
			setUndecorated(true);
			setVisible(true);
			GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
			currentImageIndex = 0;

			
			//Use displayer Thread to update what is shown on the screen
			final Displayer displayer = new Displayer();
			displayer.start();
			ActionListener slideshowChanger = new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					displayer.changeImage();
				}
			};//end slideshowChanger

			//This timer sets the interval for changing pictures (10 seconds)
			Timer timer = new Timer(10000,slideshowChanger);
			timer.setRepeats(true);
			timer.start();
		}//end SlideShowFrame constructor

		//This class changes the picture being shown on the screen
		class Displayer extends Thread {

			//changeImage changes which index of the imgs array to display next
			public void changeImage() {
				
				//Ensure there are no images downloading and that there are pictures in the Slideshow
				if(doneDownloading && imgs != null && imgs.size() > 0  ) {
					
					//Go to the beginning of the slideshow when we reach the end
					if(currentImageIndex >= imgs.size()) {
						currentImageIndex = 0;
					}
					
					System.out.println("Slideshow image " + currentImageIndex + ": " + playingImageNames.get(currentImageIndex));
					currentImage = imgs.get(currentImageIndex);
					currentImageIndex++;
					repaint();
				
				}

			}//end changeImage
		}//end Reader class	
		
		
		//Displays the currentImage to the screen
		public void paint(Graphics g) {
			if(currentImage != null) {
				g.drawImage(currentImage,0,0,this);
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
			                 sock = listenSock.accept();             //wait for connection, then continue execution
			                 BufferedReader br =    new BufferedReader(new InputStreamReader(sock.getInputStream()));
			                 String line;
			                 
			                 //Check if the message received was ForceUpdate
			                 while ((line = br.readLine()) != null) {    
			                     if(line.equals("ForceUpdate"))
			                    	 imageUpdate(true);
			                 }
			
			                 //Close BufferedReader stream
			                 br.close();
			             }
			         } catch (IOException e) {
			             e.printStackTrace();
			        }
				return 0;
			}
		
		@Override
		public void done() {
               try {
				sock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Socket done");
		}//end done
		
			};
		
		//Start the new worker
		worker.execute();
	}
}//end Signage class