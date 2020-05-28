package com.example.pocan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;
import com.jjoe64.graphview.series.DataPointInterface;


public class MainActivity extends AppCompatActivity {
    public MainActivity(){
        pc=new PocDataContainer();
        NumToShow=60;
        NumChannels=11;
        FirstExactTimeValue=-1;
        if (Props==null) {
            Props = new ChannelProps[]{
                    new ChannelProps(0xFF0080FF, "Iw", false),      //1
                    new ChannelProps(0xFFFF8000, "Ib", false),      //0
                    new ChannelProps(0xFF800000, "T", false),       //2
                    new ChannelProps(0xFF00FF00, "Ref", true),      //3
                    new ChannelProps(0xFF00FF80, "POC1", true),     //4
                    new ChannelProps(0xFF80FF00, "POC2", true),     //5
                    new ChannelProps(0xFF000000, "K", false),       //6
                    new ChannelProps(0x900080FF, "Iw(sm)", false),  //7
                    new ChannelProps(0x90FF8000, "Ib(sm)", false),  //8
                    new ChannelProps(0xFF8080FF, "BS", true),       //9
                    new ChannelProps(0xFF0000FF, "BS(sm)", true)    //10
            };
            Props[10].Visible = true;
        }
        timer=null;
        LastFile="";
        OverrideIb =false;
        OverridenIb =5.0;
        OverrideK=false;
        OverridenK=5.0;
        SettingsChanged=false;
        ReportToOpen="";
        LastOpenReport="";
        LastFileSize=0;
        SmoothDegree=8;
        LastLoadTime = (int) (System.currentTimeMillis());
    }
    ///Properties of each channel
    public class ChannelProps{
        public ChannelProps(int cl, String legend, boolean _IsBloodSugar){
            color=cl;
            Legend=legend;
            Visible=false;
            IsBloodSugar=_IsBloodSugar;
            if(legend.charAt(0)=='I')IsCurrent=true;
        }
        ///color on the graph
        public int color;
        ///The legend
        public String Legend;
        ///Currently visible on the graph
        public boolean Visible;
        ///Uses Blood Surgar scale and units
        public boolean IsBloodSugar;
        ///Use Currents scale
        public boolean IsCurrent;
    }
    ///the date read from the report
    public class PocDataElement{
        public PocDataElement(){
            values = new double[11];
            PrevRef=-1;
            NextRef=-1;
        }
        /// [0] Iw, [1] Ib, [2] Temperature, [3] Ref, [4] poc1, [5] poc2, [6] K,
        /// [7] Iw_smoothed, [8] Ib_smoothed, [9] Predicted, [10] Predicted_smoothed
        public double[] values;
        ///used forrelaxing values
        public double Temp;
        ///BS manually measured there
        public boolean IsCalibrationPoint;
        ///date/time as string
        public String Date;
        ///index of the previous manual BS measurement
        public int PrevRef;
        ///index of the next manual BS measurement
        public int NextRef;
    }
    ///rounding the time +-1 min to round values
    public String RoundDate(String s){
        return s.replace(":01",":00")
                .replace(":59",":00")
                .replace(":29",":30")
                .replace(":31",":30");

    }
    ///List of all elements from the txt report
    public class PocDataContainer{
        public PocDataContainer() {
            Elements = new ArrayList<PocDataElement>();
        }
        public ArrayList<PocDataElement> Elements;
        ///copy data from the src channel to the dst
        public void Copy(int src, int dst){
            for(int i=0;i<Elements.size();i++){
                Elements.get(i).values[dst] = Elements.get(i).values[src];
            }
        }
        ///copy data from the src channel to Temp
        public void ToTemp(int src){
            for(int i=0;i<Elements.size();i++){
                Elements.get(i).Temp = Elements.get(i).values[src];
            }
        }
        ///relax the channel
        public void Smooth(int channel, int ntimes){
            for(int i=0;i<ntimes;i++){
                ToTemp(channel);
                int nv=Elements.size();
                for(int k=1;k<nv-1;k++){
                    Elements.get(k).values[channel] = (Elements.get(k-1).Temp+Elements.get(k).Temp+Elements.get(k+1).Temp)/3.0;
                }
            }
        }
        ///predict the BS values
        public void MakePredictions(){
            int nsmooth=SmoothDegree;
            ///first, relax currents to avoid noise
            Smooth(7,nsmooth);//Iw_smoothed
            Smooth(8,nsmooth);//Ib_smoothed
            //setting default K=5.5
            for(int i=0;i<Elements.size();i++) {
                Elements.get(i).values[6] = 5.5;
            }
            //getting K for ref points using relaxed currents
            for(int i=0;i<Elements.size();i++){
                if(Elements.get(i).IsCalibrationPoint){
                    if(MainActivity.OverrideIb) {
                        Elements.get(i).values[6]=(Elements.get(i).values[7] - MainActivity.OverridenIb)/Elements.get(i).values[3];
                    }else{
                        Elements.get(i).values[6]=(Elements.get(i).values[7] - Elements.get(i).values[8])/Elements.get(i).values[3];
                    }
                }
            }
            //interpolation of K, using the linear one
            for(int i=0;i<Elements.size();i++) {
                int next=Elements.get(i).NextRef;
                int prev=Elements.get(i).PrevRef;
                if(next==-1 && prev!=-1)Elements.get(i).values[6]=Elements.get(prev).values[6];
                else if(next!=-1 && prev==-1)Elements.get(i).values[6]=Elements.get(next).values[6];
                else if(next>prev){
                    double vp=Elements.get(prev).values[6];
                    double vn=Elements.get(next).values[6];
                    double x=(i-prev)*1.0/(next-prev);
                    Elements.get(i).values[6]=vp+(vn-vp)*x;
                }
            }
            ///overriding K if need
            if(MainActivity.OverrideK){
                for(int i=0;i<Elements.size();i++) {
                    Elements.get(i).values[6]=MainActivity.OverridenK;
                }
            }
            //prediction itself
            for(int i=0;i<Elements.size();i++) {
                if(MainActivity.OverrideIb) {
                    //Predicted
                    Elements.get(i).values[9] = (Elements.get(i).values[0] - MainActivity.OverridenIb) / Elements.get(i).values[6];
                    //Predicted_smoothed
                    Elements.get(i).values[10] = (Elements.get(i).values[7] - MainActivity.OverridenIb) / Elements.get(i).values[6];
                }else{
                    //Predicted
                    Elements.get(i).values[9]=(Elements.get(i).values[0] - Elements.get(i).values[1])/Elements.get(i).values[6];
                    //Predicted_smoothed
                    Elements.get(i).values[10]=(Elements.get(i).values[7] - Elements.get(i).values[8])/Elements.get(i).values[6];
                }
            }
        }
        ///reading the txt report
        public void read(String pathname){
            try
            {
                File file=new File(pathname);    //creates a new file instance
                FileReader fr=new FileReader(file);   //reads the file
                BufferedReader br=new BufferedReader(fr);  //creates a buffering character input stream
                StringBuffer sb=new StringBuffer();    //constructs a string buffer with no characters
                String line;
                Elements.clear();
                while((line=br.readLine())!=null)
                {
                    String [] sl=line.split("\t",100);
                    PocDataElement el=new PocDataElement();
                    el.values[7] = el.values[0]=Double.valueOf(sl[3]);//Iw
                    el.values[8] = el.values[1]=Double.valueOf(sl[4]);//Ib
                    el.values[2]=Double.valueOf(sl[6]);//T
                    el.values[3]=Double.valueOf(sl[6]);//Ref
                    el.values[4]=Double.valueOf(sl[8]);//poc1
                    el.values[5]=Double.valueOf(sl[9]);//poc2
                    el.IsCalibrationPoint = Integer.valueOf(sl[7]) > 0;
                    el.Date=RoundDate(sl[2]);
                    Elements.add(el);
                }
                //set next, prev refs
                int cc=-1;
                for(int i=0;i<Elements.size();i++){
                    if(Elements.get(i).IsCalibrationPoint)cc=i;
                    Elements.get(i).PrevRef=cc;
                }
                cc=-1;
                for(int i=Elements.size()-1;i>=0;i--){
                    if(Elements.get(i).IsCalibrationPoint)cc=i;
                    Elements.get(i).NextRef=cc;
                }
                MakePredictions();
                fr.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
        ///making the graph from the channel
        public LineGraphSeries<DataPoint> GetGraph(int channel){
            DataPoint[] dataPoints = new DataPoint[Elements.size()];
            FirstExactTimeValue=-1;
            for(int i=0;i<Elements.size();i++) {
                if (FirstExactTimeValue == -1) {
                    if(Elements.get(i).Date.contains(":00")){
                        FirstExactTimeValue=i;
                        break;
                    }
                }
            }
            for(int i=0;i<Elements.size();i++){
                dataPoints[i]=new DataPoint(i-FirstExactTimeValue, Elements.get(i).values[channel]);
            }
            LineGraphSeries<DataPoint> gr = new LineGraphSeries<DataPoint>(dataPoints);
            return gr;
        }
    }
    ///the data from the txt report
    public PocDataContainer pc;
    ///amount of data items to show
    public int NumToShow;
    ///amount of data channels, now 11
    public int NumChannels;
    ///the index in data array where you have exact time XX:00
    public int FirstExactTimeValue;
    ///timer to run each several seconds
    public Timer timer;
    ///last time when graph was loaded
    public static int LastLoadTime;
    ///last loaded report
    public static String  LastFile;
    ///last loaded report file size
    public static long LastFileSize;
    ///smoothing degree
    public static int SmoothDegree;
    ///settings: "Override Ib"
    public static boolean OverrideIb;
    ///settings: "Overriden Ib"
    public static double  OverridenIb;
    ///settings: "Override K"
    public static boolean OverrideK;
    ///settings: "Overriden K"
    public static double  OverridenK;
    ///settings changed, need to reload the report
    public static boolean SettingsChanged;
    ///report that chesen manually
    public static String  ReportToOpen;
    ///last report that was open manually
    public static String  LastOpenReport;
    ///menu reference
    public Menu MenuRef;
    ///save settings to /storage/emulated/0/PocData/settings.dat
    public static void SaveSettings(){
        try {
            appendLog("MainActivity::SaveSettings");
            File file = new File("/storage/emulated/0/PocData/settings.dat");
            FileWriter fr = new FileWriter(file);
            fr.write("OverrideIb " + (OverrideIb ? "1" : "0") + "\n");
            fr.write("OverridenIb " + OverridenIb+ "\n");
            fr.write("OverrideK " + (OverrideK ? "1" : "0")+ "\n");
            fr.write("OverridenK " + OverridenK+ "\n");
            fr.flush();
            fr.close();
        }catch (Exception e){
            e.printStackTrace();
            appendLog("MainActivity::SaveSettings failed");
        }
    }
    ///load settings from /storage/emulated/0/PocData/settings.dat
    public static void LoadSettings(){
        try {
            appendLog("MainActivity::LoadSettings");
            File file = new File("/storage/emulated/0/PocData/settings.dat");
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                String[] sl = line.split(" ", 100);
                if (sl.length == 2) {
                    if (sl[0].equals("OverrideIb")) OverrideIb = Integer.valueOf(sl[1]) != 0;
                    if (sl[0].equals("OverrideKb")) OverrideK = Integer.valueOf(sl[1]) != 0;
                    if (sl[0].equals("OverridenIb"))OverridenIb = Double.valueOf(sl[1].replace(',', '.'));
                    if (sl[0].equals("OverridenK")) OverridenK = Double.valueOf(sl[1].replace(',', '.'));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            appendLog("MainActivity::LoadSettings failed");
        }
    }
    ///channelsproperties
    static ChannelProps[] Props;
    ///get reference to the channel property
    ChannelProps prop(int channel){
        return Props[channel];
    }
    //load values from the txt report, returns true if something changed
    public boolean ReadLastGraph() {
        try {
            if(ReportToOpen.length() > 0){
                if(!ReportToOpen.equals(LastOpenReport)) {
                    LastOpenReport=ReportToOpen;
                    pc.read(ReportToOpen);
                    return true;
                }
                return false;
            }
            LastOpenReport=ReportToOpen="";
            File directory = new File("/storage/emulated/0/PocData/");
            File[] files = directory.listFiles();
            appendLog("MainActivity::ReadLastGraph, directory.listFiles done");
            long Last = 0;
            boolean First = true;
            String Best = "";
            long BestSize=0;
            for (int i = 0; i < files.length; i++) {
                File myfile = files[i];
                if (myfile.getName().contains(".txt")) {
                    try {
                        long lastmodified = myfile.lastModified();
                        if (First || lastmodified > Last) {
                            First = false;
                            Last = lastmodified;
                            Best = myfile.getAbsolutePath();
                            BestSize = myfile.length();
                        }
                    } catch (Exception e) {
                        appendLog("Exception,MainActivity::ReadLastGraph[1] " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            if (Best.length() > 0 && (SettingsChanged|| BestSize!=LastFileSize || !LastFile.equals(Best))) {
                SettingsChanged=false;
                pc.read(Best);//"/storage/emulated/0/PocData/SN06900044_2020-04-25 09_18_59(21).txt");
                LastFile=Best;
                LastFileSize=BestSize;
                LastLoadTime = (int) (System.currentTimeMillis());
                return true;
            }
        } catch (Exception e) {
            appendLog("Exception,MainActivity::ReadLastGraph[2] "+e.getMessage());
            e.printStackTrace();
            CheckPermissions();
        }
        return false;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appendLog("MainActivity::onCreate");
        LoadSettings();
        CheckPermissions();
        super.onCreate(savedInstanceState);
        //requestPermissions();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Reading PocData folder...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        GraphView graph = (GraphView) findViewById(R.id.graph);
        ReadLastGraph();
        setupGraph();
        {
            int vs[]=new int[]{R.id.id_3H,R.id.id_6H,R.id.id_12H,R.id.id_24H,R.id.id_ALL};
            for(int k=0;k<5;k++) {
                final int time=k == 4 ? pc.Elements.size() : 60<<k;
                Button B = findViewById(vs[k]);
                B.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        NumToShow = time;
                        setupGraph();
                    }
                });
            }
        }
        final Handler handler = new Handler();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        if (ReadLastGraph()) {
                            setupGraph();
                        }
                    }
                });
            }
        }, 0, 4000);
    }
    @Override
    public void onResume(){
        int t = (int) (System.currentTimeMillis());
        if(t - LastLoadTime > 180000){
            LastFileSize=0;
        }
        super.onResume();
        if(ReadLastGraph()) {
            setupGraph();
        }
    }
    public void SetupMenuBoxes(){
        int[] ChIds=new int[]{R.id.show_Iw,R.id.show_Ib,-1,-1,R.id.show_Poc1,R.id.show_Poc2,R.id.show_K,R.id.smooth_Iw,R.id.smooth_Ib,R.id.show_BS, R.id.show_BS_smooth};
        for(int k=0;k<NumChannels;k++) {
            int id=ChIds[k];
            if(id!=-1 && MenuRef!=null){
                ChannelProps pr = prop(k);
                MenuItem m=(MenuItem) MenuRef.findItem(id);
                if(m!=null){
                    m.setChecked(pr.Visible);
                }
            }
        }
    }
    public void setupGraph(){
        if(pc.Elements.size() > 0) {
            GraphView graph = (GraphView) findViewById(R.id.graph);
            graph.removeAllSeries();
            boolean HasSugar=false;
            boolean HasCurrent=false;
            for(int k=0;k<NumChannels;k++) {
                ChannelProps pr = prop(k);
                if (pr.Visible) {
                    LineGraphSeries<DataPoint> series = pc.GetGraph(k);
                    series.setColor(pr.color);
                    series.setTitle(pr.Legend);
                    graph.addSeries(series);
                    graph.getViewport().setXAxisBoundsManual(true);
                    graph.getViewport().setMinX(pc.Elements.size() - NumToShow-FirstExactTimeValue);
                    graph.getViewport().setMaxX(pc.Elements.size()-FirstExactTimeValue);
                    series.setOnDataPointTapListener(new OnDataPointTapListener() {
                        @Override
                        public void onTap(Series series, DataPointInterface dataPoint) {
                            Context context = getApplicationContext();
                            Toast.makeText(context, series.getTitle() + ": " + String.format("%.02f", dataPoint.getY()), Toast.LENGTH_LONG).show();
                        }
                    });
                    if(pr.IsCurrent)HasCurrent=true;
                    if(pr.IsBloodSugar)HasSugar=true;
                    if(HasCurrent) {
                        graph.getViewport().setYAxisBoundsManual(true);
                        graph.getViewport().setMinY(0);
                        graph.getViewport().setMaxY(80);
                        graph.getGridLabelRenderer().setNumVerticalLabels(17);
                    }else
                    if(HasSugar){
                        graph.getViewport().setYAxisBoundsManual(true);
                        graph.getViewport().setMinY(0);
                        graph.getViewport().setMaxY(20);
                        graph.getGridLabelRenderer().setNumVerticalLabels(21);
                    }else{
                        graph.getViewport().setYAxisBoundsManual(true);
                        graph.getViewport().setMinY(0);
                        graph.getViewport().setMaxY(10);
                        graph.getGridLabelRenderer().setNumVerticalLabels(11);
                    }
                }
            }
            graph.getViewport().setScrollable(true); // enables horizontal scrolling
            graph.getViewport().setScalable(true); // enables horizontal zooming and scrolling
            if(HasSugar) {
                LineGraphSeries<DataPoint> low = new LineGraphSeries<DataPoint>(new DataPoint[]{
                        new DataPoint(0, 3.9),
                        new DataPoint(pc.Elements.size(), 3.9)
                });
                low.setColor(0xFFFF0000);
                low.setTitle("Low");
                graph.addSeries(low);
                LineGraphSeries<DataPoint> high = new LineGraphSeries<DataPoint>(new DataPoint[]{
                        new DataPoint(0, 10),
                        new DataPoint(pc.Elements.size(), 10)
                });
                high.setColor(0xFFC0B000);
                high.setTitle("High");
                graph.addSeries(high);
                int nref = 0;
                for (int i = 0; i < pc.Elements.size(); i++) {
                    if (pc.Elements.get(i).IsCalibrationPoint) nref++;
                }
                DataPoint[] rp = new DataPoint[nref];
                int p = 0;
                for (int i = 0; i < pc.Elements.size(); i++) {
                    if (pc.Elements.get(i).IsCalibrationPoint) {
                        rp[p] = new DataPoint(i-FirstExactTimeValue, pc.Elements.get(i).values[3]);
                        p++;
                    }
                }
                PointsGraphSeries<DataPoint> refs = new PointsGraphSeries<DataPoint>(rp);
                refs.setColor(0x80FF0000);
                refs.setSize(16);
                refs.setOnDataPointTapListener(new OnDataPointTapListener() {
                    @Override
                    public void onTap(Series series, DataPointInterface dataPoint) {
                        Context context = getApplicationContext();
                        Toast.makeText(context, "REF:" + String.format("%.02f", dataPoint.getY()), Toast.LENGTH_LONG).show();
                    }
                });
                refs.setTitle("User's");
                graph.addSeries(refs);
            }
            graph.getLegendRenderer().setVisible(true);
            graph.getLegendRenderer().setFixedPosition(0, 0);
            //graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

            graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                @Override
                public String formatLabel(double value, boolean isValueX) {
                    if (isValueX) {
                        int pos=(int)value+FirstExactTimeValue;
                        if(pos>=0 && pos<pc.Elements.size()){
                            String s = pc.Elements.get(pos).Date;
                            if(s.length()>11) {
                                s = s.substring(11);
                                if(s.equals("00:00"))s=pc.Elements.get(pos).Date.substring(5);
                            }
                            return s;
                        }
                        return "";
                    } else {
                        return super.formatLabel(value, isValueX);
                    }
                }
            });

            TextView T = (TextView)findViewById(R.id.StatusString);
            PocDataElement el = pc.Elements.get(pc.Elements.size()-1);
            ///display BS and currents
            T.setText("BS: "+String.format("%.1f", el.values[10])
                    +" K: "+String.format("%.2f", el.values[6])
                    +" Iw: "+String.format("%.1f", el.values[0])
                    +" Ib: "+String.format("%.1f", el.values[1]));
        }
        SetupMenuBoxes();
    }
    ///show options tab
    public void Options(){
        Intent intent = new Intent(MainActivity.this, SettingsPage.class);
        startActivity(intent);
    }
    ///show file selection tab
    public void SelFile(){
        Intent intent = new Intent(MainActivity.this, SelectReport.class);
        startActivity(intent);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        int vis=-1;
        switch (item.getItemId()) {
            case R.id.menu_usage:
                String url = getResources().getString(R.string.menu_usage_URL);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                break;
            case R.id.sel_file:
                SelFile();
                break;
            case R.id.id_settings:
                Options();
                break;
            case R.id.show_BS:
                vis=9;
                break;
            case R.id.show_BS_smooth:
                vis=10;
                break;
            case R.id.show_K:
                vis=6;
                break;
            case R.id.show_Iw:
                vis=0;
                break;
            case R.id.show_Ib:
                vis=1;
                break;
            case R.id.smooth_Iw:
                vis=7;
                break;
            case R.id.smooth_Ib:
                vis=8;
                break;
            case R.id.show_Poc1:
                vis=4;
                break;
            case R.id.show_Poc2:
                vis=5;
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        if(vis!=-1){
            Props[vis].Visible=!Props[vis].Visible;
        }
        setupGraph();
        return true;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        MenuRef=menu;
        SetupMenuBoxes();
        return true;
    }
    public void CheckPermissions(){
        Context context = getApplicationContext();
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},100);
        }
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},100);
        }
    }
    ///log to /storage/emulated/0/PocData/log.file
    public static void appendLog(String text) {
        return;//disable logs
        /*
        File logFile = new File("/storage/emulated/0/PocData/log.file");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        */
    }
}
