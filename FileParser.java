import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class FileParser {
	
	public FileParser() {
		
	}
	
	
	public static void main(String[] args) {
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
			rows[i] = temp[i].split(",");
			
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
			Calendar calendar = Calendar.getInstance();
			int day = calendar.get(Calendar.DAY_OF_WEEK); 
			

				int startHour = Integer.parseInt(rows[i][1]);
				if(startHour == 12) {
					startHour =0;
				}
				int startMinute = Integer.parseInt(rows[i][2]);
				int endHour = Integer.parseInt(rows[i][3]);
				int endMinute = Integer.parseInt(rows[i][4]);
				
				
				int currentDay = Integer.parseInt(rows[i][3+day]);
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
		
		
		
		
		
		
	}
	
	public void getCurrentContent() {
		
		

	}
}
