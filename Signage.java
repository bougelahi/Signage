import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class Signage {

	/*
	 * Declare variables for the slideshows
	 */
	boolean first = true;
	String schedule = ""; 
	BufferedImage[] imgs; //Array for all the images to be played
	String[] imageNames; //Array to store all of the image names

	boolean doneDownloading = false; 
	SlideShowFrame frame; //Frame for displaying the images
	int currentImage = 0; //Index of imgs currently being displayed
	BufferedImage image; //Current Image
	
	//These store the width and height of the screen connected to the Raspberry Pi
	//Used for resizing images
	static final int IMG_WIDTH = java.awt.Toolkit.getDefaultToolkit().getScreenSize().width;
	static final int IMG_HEIGHT = java.awt.Toolkit.getDefaultToolkit().getScreenSize().height;
	
	
	public Signage() {
		
		//imageUpdater calls the imageUpdate method
		//This downloads the schedule and images from the web server
		ActionListener imageUpdater = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				imageUpdate();
			}
		};//end imageUpdater
		
		//imageUpdate is called every 10 minutes
		Timer timer = new Timer(60000,imageUpdater);
		timer.setRepeats(true);
		timer.start();
		imageUpdate();
		
		//create the frame for the slideshow
		frame = new SlideShowFrame();

	}

	//imageUpdate is called by the ActionListener
	public void imageUpdate() {
		
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		String currentTime = sdf.format(new Date());
		int currentMinute = Integer.parseInt(currentTime.substring(currentTime.indexOf(":") + 1));
		
		if(currentMinute % 10 == 0 || first) {
		
		try {
			//first attempt to download the schedule to txt file
			System.out.println("Download Schedule!");
			downloadSchedule();
		}//end try
		catch(Exception e) {
			e.printStackTrace();
		}//end catch
		try {
			//Then download the images in the slideshow
			System.out.println("Download Images!");
			downloadImages();
		}//end try
		catch(Exception e) {

		}//end catch
		first = false;
		}
		else {
			
			System.out.println(currentTime + ": no update.");
		}



	}//end imageUpdate

	//resizeImage changes the downloaded image's size to the screen's resolution automatically
	//returns the resized Image
	BufferedImage resizeImage(BufferedImage originalImage, int type) {
		BufferedImage resizedImage = new BufferedImage(IMG_WIDTH,IMG_HEIGHT,type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage,0,0,IMG_WIDTH,IMG_HEIGHT,null);

		return resizedImage;
	}//end resizeImage

	
	//downloadSchedule fetches the schedule from the server
	//and saves it to a text file
	public void downloadSchedule() {
		
		ArrayList<String> imageNameList = new ArrayList<String>();
		try {
			//First connect to the server
			URL url = new URL("http://fsa-web02.food.uga.edu/signage/fetch_images.php");
			HttpURLConnection con = (HttpURLConnection)(url.openConnection());
			int responseCode = con.getResponseCode();
			System.out.println("Sending 'GET' request to URL : " + url);
			System.out.println("Response Code : " + responseCode);

			
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
			//Read in the text from the server one line at a time
			while((inputLine = in.readLine()) != null) {
				
				//response stores all of the text to save to the text file
				response.append(inputLine+"\r\n");
				String[] arr = inputLine.split(",");
				
				if(arr.length > 1 && arr[0].length() > 1) {
					//store image names to download images from the server
					imageNameList.add(arr[0].substring(1));
				}

			}//end while
			
			in.close();
			//Store the response in a single string
			schedule = response.toString();
			System.out.println(schedule);
			
			//BufferedWriter used to write text to a file
			BufferedWriter writer = null;
			writer = new BufferedWriter( new FileWriter("schedule.txt"));
			//Write the string to the file
			writer.write(schedule);

			if ( writer != null) {
				writer.close( );
			}//end if
			
			//Store image names in the instance variable array
			imageNames = new String[imageNameList.size()];
			imageNameList.toArray(imageNames);




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
				for(int i = 0; i < imageNames.length; i++) {
					try {
						//Connect to the server using the image names in the URL to download them
						System.out.println("Writing image: " + imageNames[i].substring(imageNames[i].lastIndexOf("/")+1));
						URL url = new URL("http://fsa-web02.food.uga.edu/signage" +imageNames[i]);
						temp_image = ImageIO.read(url);
						type = temp_image.getType() == 0? BufferedImage.TYPE_INT_ARGB : temp_image.getType();
					
						//Place resized Image inside of the temporary array
						temp_image = resizeImage(temp_image,type); //Resize
						if(imageNames[i].lastIndexOf("/") != -1) {
							ImageIO.write(temp_image, "jpg", new File(imageNames[i].substring(imageNames[i].lastIndexOf("/")+1))); 
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
				
				//get the array of images to be played at this moment
				imgs=getCurrentContent();




			}//end done

		};//end worker
		worker.execute();
	}//end downloadImages


	//This method reads the "schedule.txt" file to get the list of currently
	//playing images in the slideshow throughout the day
	public BufferedImage[] getCurrentContent() {
		ArrayList<BufferedImage> imageList = new ArrayList<BufferedImage>();
		ArrayList<String> names = new ArrayList<String>();
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
						SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
						Calendar calendar = Calendar.getInstance();
						
						//Get the information from the file
						int day = calendar.get(Calendar.DAY_OF_WEEK); 
						int startHour = Integer.parseInt(attrs[1]);
						int startMinute = Integer.parseInt(attrs[2]);
						int endHour = Integer.parseInt(attrs[3]);
						int endMinute = Integer.parseInt(attrs[4]);

						//Get actual day, time, etc.
						int currentDay = Integer.parseInt(attrs[4+day]);
						String currentTime = sdf.format(new Date());
						int currentHour = Integer.parseInt(currentTime.substring(0, currentTime.indexOf(":")));
						int currentMinute = Integer.parseInt(currentTime.substring(currentTime.indexOf(":") + 1));

						// (currentHour > startHour AND currentHour < endHour) OR
						// (currentHour = startHour AND currentMinute >= startMinute) OR
						// (currentHour = startHour AND currentHour = endHour AND currentMinute >= startMinute AND currentMinute <= endMinute)
						/*
				System.out.println("Current Time: " + currentHour +":"+currentMinute + "  Day: " + currentDay);
				System.out.println("Start: " + startHour+":" +startMinute );
				System.out.println("End: " + endHour+":"+endMinute);
				System.out.println((currentHour == startHour ) );
				System.out.println((currentMinute >= startMinute));
						 */
						//Compare the actual day/time with the required days/times in the schedule
						//to determine what should play right now
						if(currentDay == 1 && 
						((currentHour > startHour && currentHour < endHour) ||
						(currentHour == startHour && currentMinute >= startMinute) || 
						(currentHour == startHour && currentHour == endHour && currentMinute >= startMinute && currentMinute <= endMinute)||
						( currentHour == endHour && currentMinute <= endMinute))){

							System.out.println(attrs[0].substring(attrs[0].lastIndexOf("/")+1) + " IS playing!");
							if(attrs[0].lastIndexOf("/") != -1){

								imageList.add(ImageIO.read(new File(attrs[0].substring(attrs[0].lastIndexOf("/")+1))));
								names.add(attrs[0].substring(attrs[0].lastIndexOf("/")+1));
							}//end if
						}//end outer if
						else {
							System.out.println(attrs[0].substring(attrs[0].lastIndexOf("/")+1) + " IS NOT playing!");
						}//end else

					}//end if
				}//end wile
			}//end if
			br.close();
		}//end try

		catch(Exception e) {
			e.printStackTrace();
		}//end catch
		
		//Store the results and names in arrays
		BufferedImage[] temp = new BufferedImage[imageList.size()];
		imageList.toArray(temp);
		
		imageNames = new String[names.size()];
		names.toArray(imageNames);
		imgs = temp;
		//return the result
		return temp;

	}//end getCurrentContent
	
	//This class contains the code for the Frame/Window of the program
	class SlideShowFrame extends JFrame {

		/**
		 * 
		 */
		private static final long serialVersionUID = -8629357630980989062L;

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
				if(doneDownloading && imgs != null && imgs.length != 0  ) {
					if(currentImage >= imgs.length) {
						currentImage = 0;
					}//end if
					try {
						System.out.println("Setting image to " + currentImage + ": " + imageNames[currentImage]);//+ "  " + imageNames[currentImage]);
						setImage(imgs[currentImage]);
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
			if(newImage != null) {
				image = newImage;
				repaint();
			}//end if
		}//end setImage
		
		//This is called with the repaint method.
		//it simply displays the variable 'image' to the screen
		public void paint(Graphics g) {
			if(image != null) {
				g.drawImage(image,0,0,this);
			}//end if
		}//end paint


	}//end SlideShowFrame class
	
	public static void main(String[] args) {
		Signage f = new Signage();
		f.downloadSchedule();

	}//end main
}//end Signage class
