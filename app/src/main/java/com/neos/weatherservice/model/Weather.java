package com.neos.weatherservice.model;


public class Weather {
	
	public Location location;
	public final CurrentCondition currentCondition;
	public final Temperature temperature;
	public final Wind wind;

	public Weather() {
		currentCondition = new CurrentCondition();
		temperature = new Temperature();
		wind = new Wind();
	}
//	public Rain rain = new Rain();
//	public Snow snow = new Snow()	;
//	public Clouds clouds = new Clouds();
	
//	public byte[] iconData;
	
	public class CurrentCondition {
		private int weatherId;
		private String condition;
		private String descr;
		private String icon;
		private String icon_url;
		
		
		private float pressure;
		private String humidity;

		public int getWeatherId() {
			return weatherId;
		}
		public void setWeatherId(int weatherId) {
			this.weatherId = weatherId;
		}
		public String getCondition() {
			return condition;
		}
		public void setCondition(String condition) {
			this.condition = condition;
		}
		public String getDescr() {
			return descr;
		}
		public void setDescr(String descr) {
			this.descr = descr;
		}
		public String getIcon() {
			return icon;
		}
		public void setIconUrl(String icon_url) {
			this.icon_url = icon_url;
		}
		public String getIconUrl() {
			return icon_url;
		}
		public void setIcon(String icon) {
			this.icon = icon;
		}
		public float getPressure() {
			return pressure;
		}
		public void setPressure(float pressure) {
			this.pressure = pressure;
		}
		public String getHumidity() {
			return humidity;
		}
		public void setHumidity(String humidity) {
			this.humidity = humidity;
		}
		
		
	}

	
	public class Temperature {
		private float temp;
		private float tempF;
//		private float minTemp;
//		private float maxTemp;
		
		public float getTemp() {
			return temp;
		}
		public void setTemp(float temp) {
			this.temp = temp;
		}
		public float getTempF() {
			return tempF;
		}
		public void setTempF(float tempF) {
			this.tempF = tempF;
		}
/*		public float getMinTemp() {
			return minTemp;
		}
		public void setMinTemp(float minTemp) {
			this.minTemp = minTemp;
		}
		public float getMaxTemp() {
			return maxTemp;
		}
		public void setMaxTemp(float maxTemp) {
			this.maxTemp = maxTemp;
		}
		*/
	}
	
	public class Wind {
		private float speed;
		private float deg;
		public float getSpeed() {
			return speed;
		}
		public void setSpeed(float speed) {
			this.speed = speed;
		}
		public float getDeg() {
			return deg;
		}
		public void setDeg(float deg) {
			this.deg = deg;
		}
		
		
	}
	
/*	public  class Rain {
		private String time;
		private float ammount;
		public String getTime() {
			return time;
		}
		public void setTime(String time) {
			this.time = time;
		}
		public float getAmmount() {
			return ammount;
		}
		public void setAmmount(float ammount) {
			this.ammount = ammount;
		}
		
		
		
	}

	public  class Snow {
		private String time;
		private float ammount;
		
		public String getTime() {
			return time;
		}
		public void setTime(String time) {
			this.time = time;
		}
		public float getAmmount() {
			return ammount;
		}
		public void setAmmount(float ammount) {
			this.ammount = ammount;
		}
		
		
	}
	
	public  class Clouds {
		private int perc;

		public int getPerc() {
			return perc;
		}

		public void setPerc(int perc) {
			this.perc = perc;
		}
		
		
	}*/

}
