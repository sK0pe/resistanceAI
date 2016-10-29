import cits3001_2016s2.*;
import s21469477.*;

import java.io.*;

public class Run{

 public static void main(String args[]){

  try{
      File f = new File("Results.html");
      FileWriter fw = new FileWriter(f);
      Competitor[] contenders = {
        new Competitor(new s21469477.DimitriDevil(),"DimitriDevil","Pradyumn"),
        new Competitor(new s21469477.HeuristicAgent(),"HeuristicHarry","Pradyumn")
        };
      fw.write(Game.tournament(contenders, 500));
      fw.close();
    }
    catch(IOException e){System.out.println("IO fail");}
  }

}  


