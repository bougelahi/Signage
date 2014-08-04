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

	String schedule = "";
	BufferedImage[] imgs;
	String[] imageNames;

	boolean doneDownloading = false;
	SlideShowFrame frame;
	int currentImage = 0;
	BufferedImage image;
	static final int IMG_WIDTH = java.awt.Toolkit.getDefaultToolkit().getScreenSize().width;
	static final int IMG_HEIGHT = java.awt.Toolkit.getDefaultToolkit().getScreenSize().height;
	public Signage() {
		ActionListener imageUpdater = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				imageUpdate();
			}
		};//end imageUpdater
		Timer timer = new Timer(600000,imageUpdater);
		timer.setRepeats(true);
		timer.start();
		imageUpdate();
		frame = new SlideShowFrame();

	}


	public void imageUpdate() {
		try {
			System.out.println("Download Schedule!");
			downloadSchedule();


		}//end try
		catch(Exception e) {
			e.printStackTrace();
		}//end catch
		try {
			
			System.out.println("Download Images!");
			downloadImages();
		}//end try
		catch(Exception e) {

		}//end catch

	}//end imageUpdate

	BufferedImage resizeImage(BufferedImage originalImage, int type) {
		BufferedImage resizedImage = new BufferedImage(IMG_WIDTH,IMG_HEIGHT,type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage,0,0,IMG_WIDTH,IMG_HEIGHT,null);

		return resizedImage;
	}//end resizeImage


	public void downloadSchedule() {
		
		ArrayList<String> imageNameList = new ArrayList<String>();
		try {
			//URL url = new URL("http://fsa-web02.food.uga.edu/signage/fetch_images.php");
			URL url = new URL("http://128.192.109.15/signage/fetch_images.php");
			HttpURLConnection con = (HttpURLConnection)(url.openConnection());
			int responseCode = con.getResponseCode();
			System.out.println("Sending 'GET' request to URL : " + url);
			System.out.println("Response Code : " + responseCode);


			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while((inputLine = in.readLine()) != null) {
				//System.out.println(inputLine);
				response.append(inputLine+"\r\n");
				String[] arr = inputLine.split(",");
				if(arr.length > 1 && arr[0].length() > 1) {
				//System.out.println(arr[0].substring(1) + " added to imageNames");
				imageNameList.add(arr[0].substring(1));
				}

			}//end while

			in.close();
			System.out.println(response.toString());
			schedule = response.toString();
			BufferedWriter writer = null;
			writer = new BufferedWriter( new FileWriter("schedule.txt"));
			writer.write(response.toString());

			if ( writer != null) {
				writer.close( );
			}//end if
		imageNames = new String[imageNameList.size()];
			imageNameList.toArray(imageNames);




		}//end try 
		catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//end MalformedURLException catch
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//end IOExcception catch
		
	}//end downloadSchedule

	public void downloadImages() {
		doneDownloading = false;
		SwingWorker worker = new SwingWorker<BufferedImage[], Void>() {
			@Override
			public BufferedImage[] doInBackground() {
				BufferedImage[] temp = new BufferedImage[imageNames.length];
				int type = 0;
				for(int i = 0; i < imageNames.length; i++) {
					try {
						System.out.println("Writing image: " + imageNames[i].substring(imageNames[i].lastIndexOf("/")+1));
						//URL url = new URL("http://fsa-web02.food.uga.edu/signage" +imageNames[i]);
						URL url = new URL("http://128.192.109.15/signage" +imageNames[i]);
						BufferedImage image = ImageIO.read(url);
						type = image.getType() == 0? BufferedImage.TYPE_INT_ARGB : image.getType();
						temp[i] = resizeImage(image,type); //TODO: Resize
						if(imageNames[i].lastIndexOf("/") != -1) {
							ImageIO.write(temp[i], "jpg", new File(imageNames[i].substring(imageNames[i].lastIndexOf("/")+1))); 
						}//end if


					}//end try
					catch(Exception e) {
						e.printStackTrace();
					}//end catch
				}//end for


				return temp;
			}//end doInBackground

			@Override
			public void done() {

				System.out.println("Writing done");
				//	imgs = get();
				doneDownloading = true;
				imgs=getCurrentContent();



			}//end done

		};//end worker
		worker.execute();
	}//end downloadImages

	public static void main(String[] args) {
		Signage f = new Signage();
		f.downloadSchedule();

	}//end main

	public BufferedImage[] getCurrentContent() {
		ArrayList<BufferedImage> imageList = new ArrayList<BufferedImage>();
		ArrayList<String> names = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader("schedule.txt"));
			String line;
			String[] attrs;
			int i = 0;

			while ((line = br.readLine()) != null) {
				
				if(line.contains(",")) {
					//System.out.println("line: " + line);
				attrs = line.split(",");

				if(attrs.length >= 5) {
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
				Calendar calendar = Calendar.getInstance();
				int day = calendar.get(Calendar.DAY_OF_WEEK); 
			//	System.out.println("Current Day: " + day);

				int startHour = Integer.parseInt(attrs[1]);

				int startMinute = Integer.parseInt(attrs[2]);
				int endHour = Integer.parseInt(attrs[3]);
				int endMinute = Integer.parseInt(attrs[4]);


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
				if(currentDay == 1 && ((currentHour > startHour && currentHour < endHour) ||
						(currentHour == startHour && currentMinute >= startMinute) || 
						(currentHour == startHour && currentHour == endHour && currentMinute >= startMinute && currentMinute <= endMinute)||
						( currentHour == endHour && currentMinute <= endMinute))){

					System.out.println(attrs[0].substring(attrs[0].lastIndexOf("/")+1) + " IS playing!");
					if(attrs[0].lastIndexOf("/") != -1)
					//	System.out.println(attrs[0].substring(attrs[0].lastIndexOf("/")+1));
						imageList.add(ImageIO.read(new File(attrs[0].substring(attrs[0].lastIndexOf("/")+1))));
					names.add(attrs[0].substring(attrs[0].lastIndexOf("/")+1));
					
				}
				else {
					//System.out.println("CurrentDay: " + currentDay);
					//System.out.println("Start: " + startHour+":" +startMinute + "\tEnd: " + endHour+":"+endMinute);
				System.out.println(attrs[0].substring(attrs[0].lastIndexOf("/")+1) + " IS NOT playing!");
				
			}//end else

				}//end if
				
				i++;
			}//end wile
		}//end if
		}//end try

		catch(Exception e) {
			e.printStackTrace();
		}//end catch

		BufferedImage[] temp = new BufferedImage[imageList.size()];
		imageList.toArray(temp);
		
		imageNames = new String[names.size()];
		names.toArray(imageNames);
		imgs = temp;
		return temp;

	}//end getCurrentContent

	class SlideShowFrame extends JFrame {

		public SlideShowFrame() {
			super();
			setUndecorated(true);
			setVisible(true);
			GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
			currentImage = 0;
			final Reader reader = new Reader();
			reader.start();
			ActionListener slideshowChanger = new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					reader.changeImage();
				}//end actionPerformed
			};//end slideshowChanger
			Timer timer = new Timer(10000,slideshowChanger);
			timer.setRepeats(true);
			timer.start();
		}//end SlideShowFrame constructor




		class Reader extends Thread {

			public void run() {

			}//end run

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

		public void setImage(BufferedImage newImage) {
			if(newImage != null) {
				image = newImage;
				repaint();
			}//end if
		}//end setImage

		public void paint(Graphics g) {
			if(image != null) {
				g.drawImage(image,0,0,this);
			}//end if
		}//end paint


	}//end SlideShowFrame class
}//end Signage class
