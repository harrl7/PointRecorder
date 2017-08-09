package bit.harrl7.pointrecorder;

/**
 * Created by Liam on 09-Aug-17.
 */

public class WorldPoint
{
    double lat;
    double lng;
    String label;

    public WorldPoint() {} // Empty

    public String ToCSV()
    {
        return label + ", " + lat + ", " + lng;
    }


    public double getLat() {return lat;}
    public void setLat(double lat) {this.lat = lat;}

    public double getLng() {return lng;}
    public void setLng(double lng) {this.lng = lng;}

    public String getLabel() {return label;}
    public void setLabel(String label) {this.label = label;}
}
