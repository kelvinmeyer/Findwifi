package com.gmail.kelvinmeyer13.findwifi;

import com.google.android.gms.maps.model.LatLng;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by kelvin on 30/03/2016.
 */
public class PlaceLocation {
    private String name;
    private String oTime;
    private String cTime;
    private String bestReception;
    private LatLng coords;
    private String passwordInfo;
    private String service;
    private int distance;
    private String direction;
    private int id;

    public PlaceLocation(String name, String oTime, String cTime, String bestReception, double lat, double lng, String passwordInfo, String passwordControl, String service, int distance, String direction,int id) {
        this.name = name;
        this.oTime = oTime;
        this.cTime = cTime;
        this.bestReception = bestReception;
        this.coords = new LatLng(lat,lng);
        this.passwordInfo = passwordInfo+" - "+passwordControl;
        this.service = service;
        this.distance = distance;
        this.direction = direction;
        this.id = id;
    }

    public Date getTime(){
        Calendar rightNow = Calendar.getInstance();
        return rightNow.getTime();
    }

    public int openTime(){
        if(!oTime.equalsIgnoreCase("Unknown")) {
            if (oTime.substring(oTime.indexOf(" ") + 1, oTime.length()).equalsIgnoreCase("pm")) {
                try {
                    return Integer.parseInt(oTime.substring(0, oTime.indexOf(" "))) + 11;
                }catch(Exception e){
                    return 0;
                }
            } else {
                try {
                    return Integer.parseInt(oTime.substring(0, oTime.indexOf(" "))) - 1;
                }catch(Exception e){
                    return 0;
                }
            }
        }
        else{
            return -1;
        }
    }

    public int closeTime(){
        if(!oTime.equalsIgnoreCase("Unknown")) {
            if (cTime.substring(cTime.indexOf(" ")+1,cTime.length()).equalsIgnoreCase("pm")) {
                try{
                    return Integer.parseInt(cTime.substring(0, cTime.indexOf(" ")))+11;
                }catch(Exception e){
                    return 0;
                }
            }
            else{
                try{
                    return Integer.parseInt(cTime.substring(0, cTime.indexOf(" ")))-1;
                }catch(Exception e){
                    return 0;
                }
            }
        }
        else{
            return -1;
        }
    }


    public boolean isOpen(){
        int opentime = openTime();
        int closetime = closeTime();
        if(opentime == -1||closetime==-1){
            return true;
        }
        else {
            Date now = getTime();
            if (opentime < now.getHours() && closetime > now.getHours()) {
                return true;
            } else {
                return false;
            }
        }
    }


    public String getName() {
        return name;
    }

    public String getTimes(){
        return oTime+" - "+cTime;
    }

    public String getDirDist() {
        return distance+"m "+direction;
    }

    public String getPasswordInfo(){
        return passwordInfo;
    }

    public String getBestSpot(){
        return bestReception;
    }

    public String getService(){
        return service;
    }

    public LatLng getCoords() {
        return coords;
    }

    public double getLat(){
        return coords.latitude;
    }

    public double getLng() {
        return coords.longitude;
    }

    public int getId() {
        return id;
    }

    //string for itent transfer
    public String toStringLong(){
        return name+"%"+oTime+"%"+cTime+"%"+bestReception+"%"+Double.toString(coords.latitude)+"%"+Double.toString(coords.longitude)+"%"+passwordInfo+"%"+service+"%"+Integer.toString(distance)+"%"+direction;
    }

    public PlaceLocation(String data){
        //name
        this.name = data.substring(0, data.indexOf("%"));
        data = data.substring(data.indexOf("%")+1);
        //oTime
        this.oTime = data.substring(0,data.indexOf("%"));
        data = data.substring(data.indexOf("%")+1);
        //cTime
        this.cTime = data.substring(0,data.indexOf("%"));
        data = data.substring(data.indexOf("%")+1);
        //bestReception
        this.bestReception = data.substring(0,data.indexOf("%"));
        data = data.substring(data.indexOf("%")+1);
        //coords
        double lat = Double.parseDouble(data.substring(0,data.indexOf("%")));
        data = data.substring(data.indexOf("%")+1);
        double lng = Double.parseDouble(data.substring(0,data.indexOf("%")));
        data = data.substring(data.indexOf("%")+1);
        coords = new LatLng(lat,lng);
        //passwordInfo
        this.passwordInfo = data.substring(0,data.indexOf("%"));
        data = data.substring(data.indexOf("%")+1);
        //service
        this.service = data.substring(0, data.indexOf("%"));
        data = data.substring(data.indexOf("%")+1);
        //distance
        this.distance = Integer.parseInt(data.substring(0,data.indexOf("%")));
        data = data.substring(data.indexOf("%")+1);
        //direction
        this.direction = data;
    }

}