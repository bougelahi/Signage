import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingWorker;
import javax.swing.Timer;


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
		};
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
			
	
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		try {
			System.out.println("Download Images!");
			downloadImages();
		}
		catch(Exception e) {
			
		}
		
	}
	
	BufferedImage resizeImage(BufferedImage originalImage, int type) {
		BufferedImage resizedImage = new BufferedImage(IMG_WIDTH,IMG_HEIGHT,type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage,0,0,IMG_WIDTH,IMG_HEIGHT,null);
		
		return resizedImage;
	}
	
	
	public void downloadSchedule() {
		String url = "http://fsa-web02.food.uga.edu/signage/fetch_images.php";
		ArrayList<String> imageNameList = new ArrayList<String>();
		try {
			HttpURLConnection con = (HttpURLConnection)(new URL(url).openConnection());
			int responseCode = con.getResponseCode();
			System.out.println("Sending 'GET' request to URL : " + url);
			System.out.println("Response Code : " + responseCode);
			
			
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
			
			while((inputLine = in.readLine()) != null) {
			//	System.out.println(inputLine);
				response.append(inputLine+"\r\n");
				String[] arr = inputLine.split(",");
				imageNameList.add(arr[0].substring(1));
				
			}
			
			in.close();
			System.out.println(response.toString());
			schedule = response.toString();
			BufferedWriter writer = null;
			writer = new BufferedWriter( new FileWriter("schedule.txt"));
		    writer.write(response.toString());
		    
		    if ( writer != null)
		        writer.close( );
			imageNames = new String[imageNameList.size()];
		    imageNameList.toArray(imageNames);
		    
		   
		  
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
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
						URL url = new URL("http://fsa-web02.food.uga.edu/signage" +imageNames[i]);
						BufferedImage image = ImageIO.read(url);
						type = image.getType() == 0? BufferedImage.TYPE_INT_ARGB : image.getType();
						temp[i] = resizeImage(image,type); //TODO: Resize
						if(imageNames[i].lastIndexOf("/") != -1) {
							ImageIO.write(temp[i], "jpg", new File(imageNames[i].substring(imageNames[i].lastIndexOf("/")+1))); 
						}
						
						
					}
					catch(Exception e) {
						//e.printStackTrace();
					}
				}
				
				
				return temp;
			}
			
			@Override
			public void done() {
				
					System.out.println("Writing done");
				//	imgs = get();
					doneDownloading = true;
					imgs=getCurrentContent();
					
				
			
			}
			
		};
		worker.execute();
	}
	
	public static void main(String[] args) {
		Signage f = new Signage();
		f.downloadSchedule();
	/*
		String input = "./uploads/Mkt_dining_on_MP.jpg,12,0,30,0,0,1,1,1,1,1,0,\n"
				+ "./uploads/MKT_coupon_program.jpg,12,0,30,0,0,1,1,1,1,1,0,\n"
				+ "./uploads/Mkt_gift_o_grams.jpg,12,0,30,0,0,1,1,1,1,1,0,\n"
				+ "./uploads/MKT_catering-2.jpg,12,0,30,0,0,1,1,1,1,1,0,\n"
				+ "./uploads/MKT_growingly_green.jpg,12,0,30,0,0,1,1,1,1,1,0,\n"
				+ "./uploads/MKT_Georgios.jpg,12,0,30,0,0,1,1,1,1,1,0,\n"
				+ "./uploads/scott_hall_promo.jpg,12,0,30,0,0,1,1,1,1,1,0,\n"
				+ "./uploads/Marketing_RateSheet.jpg,12,0,30,0,0,1,1,1,1,1,0,\n"
				+ "./uploads/dawgpause_tate.jpg,12,0,30,0,0,1,1,1,1,1,0,\n"
				+ "./uploads/campus_eateries_list.jpg,12,0,30,0,0,1,1,1,1,1,0,\n"
				+ "./uploads/Mkt_retail_enews.jpg,12,0,30,0,0,1,1,1,1,1,0,\n"
				+ "./uploads/Checkindealsblue.jpg,12,0,30,0,0,1,1,1,1,1,0,\n"
				+ "./uploads/MKT_2.jpg,12,0,30,0,0,1,1,1,1,1,0,";
		
		String[] temp = input.split("\n");
		String[][] rows = new String[20][20];
		for(int i = 0; i < temp.length; i++) {
			attrs = temp[i].split(",");
			
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
			Calendar calendar = Calendar.getInstance();
			int day = calendar.get(Calendar.DAY_OF_WEEK); 
			

				int startHour = Integer.parseInt(attrs[1]);
				if(startHour == 12) {
					startHour =0;
				}
				int startMinute = Integer.parseInt(attrs[2]);
				int endHour = Integer.parseInt(attrs[3]);
				int endMinute = Integer.parseInt(attrs[4]);
				
				
				int currentDay = Integer.parseInt(attrs[3+day]);
				String currentTime = sdf.format(new Date());
				int currentHour = Integer.parseInt(currentTime.substring(0, currentTime.indexOf(":")));
				int currentMinute = Integer.parseInt(currentTime.substring(currentTime.indexOf(":") + 1));
				
				// (currentHour > startHour AND currentHour < endHour) OR
				// (currentHour = startHour AND currentMinute >= startMinute) OR
				// (currentHour = startHour AND currentHour = endHour AND currentMinute >= startMinute AND currentMinute <= endMinute)
		
			if(currentDay == 1 && (currentHour > startHour && currentHour < endHour) || (currentHour == startHour && currentHour == endHour && currentMinute >= startMinute && currentMinute <= endMinute)) {
					
				System.out.println(i+ " SlideShow is playing!");
			}
			else {
				System.out.println("Current Day= " + currentDay);
				System.out.println("Current time= " + currentHour+":"+currentMinute);
				System.out.println("Start time= " + startHour+":"+startMinute);
				System.out.println("End time= " + endHour+":"+endMinute);
			}
				
		
		
		
		}
		
		*/	
		
		
		
		
	}
	
	public BufferedImage[] getCurrentContent() {
		ArrayList<BufferedImage> imageList = new ArrayList<BufferedImage>();
		try {
			BufferedReader br = new BufferedReader(new FileReader("schedule.txt"));
			String line;
			String[] attrs;
			int i = 0;
		
			while ((line = br.readLine()) != null) {
			//	System.out.println(line);
				attrs = line.split(",");
				

				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
				Calendar calendar = Calendar.getInstance();
				int day = calendar.get(Calendar.DAY_OF_WEEK); 
				System.out.println("Current Day: " + day);

					int startHour = Integer.parseInt(attrs[1]);
				
					int startMinute = Integer.parseInt(attrs[2]);
					int endHour = Integer.parseInt(attrs[3]);
					int endMinute = Integer.parseInt(attrs[4]);
					
					
					int currentDay = Integer.parseInt(attrs[4+day]);
					String currentTime = sdf.format(new Date());
					int currentHour = Integer.parseInt(currentTime.substring(0, currentTime.indexOf(":")));
					//currentHour = 9;
					int currentMinute = Integer.parseInt(currentTime.substring(currentTime.indexOf(":") + 1));
					
					// (currentHour > startHour AND currentHour < endHour) OR
					// (currentHour = startHour AND currentMinute >= startMinute) OR
					// (currentHour = startHour AND currentHour = endHour AND currentMinute >= startMinute AND currentMinute <= endMinute)
			
				if(currentDay == 1 && (currentHour > startHour && currentHour < endHour) || (currentHour == startHour && currentHour == endHour && currentMinute >= startMinute && currentMinute <= endMinute)) {
						
					System.out.println(attrs[0].substring(attrs[0].lastIndexOf("/")+1) + " SlideShow is playing!");
					if(attrs[0].lastIndexOf("/") != -1)
					imageList.add(ImageIO.read(new File(attrs[0].substring(attrs[0].lastIndexOf("/")+1))));
					
				}
				else {
				
				  System.out.println(attrs[0].substring(attrs[0].lastIndexOf("/")+1) + " is not playing!");
					/*
				  System.out.println("Current Day= " + currentDay);
					System.out.println("Current time= " + currentHour+":"+currentMinute);
					System.out.println("Start time= " + startHour+":"+startMinute);
					System.out.println("End time= " + endHour+":"+endMinute);
				*/
				}
				
				
				i++;
				
				
				
			}
		}
		
		catch(Exception e) {
			e.printStackTrace();
		}
		
		BufferedImage[] temp = new BufferedImage[imageList.size()];
		imageList.toArray(temp);
		imgs = temp;
		return temp;

	}
	
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
				}
			};
			Timer timer = new Timer(10000,slideshowChanger);
			timer.setRepeats(true);
			timer.start();
		}
		
		
	
	
	class Reader extends Thread {
		
		public void run() {
			
		}
		
		public void changeImage() {
			if(doneDownloading && imgs != null && imgs.length != 0 ) {
				if(currentImage >= imgs.length) {
					currentImage = 0;
				}
				try {
					System.out.println("Setting image to " + currentImage);//+ "  " + imageNames[currentImage]);
					setImage(imgs[currentImage]);
					currentImage++;
				}
				catch(Exception e) {
					e.printStackTrace();
				}
		}
		
		}
	}	
		
		public void setImage(BufferedImage newImage) {
			if(newImage != null) {
				image = newImage;
				repaint();
			}
		}
		
		public void paint(Graphics g) {
			if(image != null) {
				g.drawImage(image,0,0,this);
			}
		}
		
	
	}
}
