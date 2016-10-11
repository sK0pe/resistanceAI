import java.util.*;
import java.io.*;

/**
 * Created by Pradyumn on 3/10/2016.
 */
public class HeuristicAgent implements Agent{
    private String name;
    private String players = "";
    private String spies = "";
    private int numPlayers;
    // Am I a spy?
    private boolean spy;
    // Missions failed up till current update
    int failedMissions = 0;
    // What mission am I playing?
    int mission = 0;
    boolean lastMissionFailed = false;
    // How to write out in Java without being overly verbose
    private PrintStream display;
    // Suspicion container
    private TreeMap<String, Double> suspicion = new TreeMap<String, Double>();
    // N Choose K (N!/(K! * (N-K)!) preprocessor
    //private int suspicionSize[] = {10, 15, 35, 56, 84, 210};
    private int suspicionDiscountSelf[] = {6, 10, 20, 35, 56, 126};

    // Printing out
    public HeuristicAgent(){
        this.display = System.out;
    }
    private void write(String s){
        display.println(s);
    }


    /**
     * become_suspicious
     *
     * Helper function for get_status
     * Initialises the suspicion container based on player size
     */
    private void become_suspicious(TreeMap<String, Double> suspicion, String players){
        //Sort the players string as not guaranteed
        char playerArr[] = players.toLowerCase().toCharArray();
        Arrays.sort(playerArr);

        StringBuilder spyCombo = new StringBuilder();
        for(int i = 0; i < numPlayers; ++i){
            for(int j = i+1; j < numPlayers; ++j){
                // 5 or 6 player game 2 spies
                if(numPlayers < 7){
                    spyCombo.setLength(0);
                    spyCombo.append(playerArr[i]).append(playerArr[j]);
                    suspicion.put(spyCombo.toString(), 0.0);
                }
                else{
                    for(int k = j + 1; k < numPlayers; ++k){
                        // 7, 8 or 9 player game 3 spies
                        if(numPlayers < 10){
                            spyCombo.setLength(0);
                            spyCombo.append(playerArr[i]).append(playerArr[j]).append(playerArr[k]);
                            suspicion.put(spyCombo.toString(), 0.0);
                        }
                        else{
                            // 10 player game, 4 spies
                            for(int l = k + 1; l < numPlayers; ++l){
                                spyCombo.setLength(0);
                                spyCombo.append(playerArr[i]).append(playerArr[j]).append(playerArr[k]).append(playerArr[l]);
                                suspicion.put(spyCombo.toString(), 0.0);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Reports the current status, including players name, the name of all players, the names of the spies (if known), the mission number and the number of failed missions
     *
     * @param name     a string consisting of a single letter, the agent's names.
     * @param players  a string consisting of one letter for everyone in the game.
     * @param spies    a String consisting of the latter name of each spy, if the agent is a spy, or n questions marks where n is the number of spies allocated; this should be sufficient for the agent to determine if they are a spy or not.
     * @param mission  the next mission to be launched
     * @param failures the number of failed missions
     * @return within 100ms
     */
    @Override
    public void get_status(String name, String players, String spies, int mission, int failures){
        // Initialise
        if(mission == 1) {
            this.name = name;
            // Player string provided from Game
            this.players = players;
            this.numPlayers = players.length();
            // Spy string provided from Game
            this.spies = spies;
            // If spy string contains my name, I'm a spy
            this.spy = spies.contains(name);
            // Initialise Suspicion
            become_suspicious(suspicion, players);
        }

        // Update mission number
        this.mission = mission;

        // Check if last mission has failed or not
        lastMissionFailed = failures > failedMissions;
        // Update failed missions
        failedMissions = failures;

        // Test data to track.
        write("Harry is playing in a " + numPlayers + "game");
        write("Harry's character name is " + name);
        write("Players in this game are " + players);
        if(spy){
            write("Spy buddies are " + spies);
        }
        else {
            write("There are " + spies.length() + "unknown spies!");
        }
        write("The upcoming mission is " + mission);
        write("So far " + failures + " missions have been failed");
        if(lastMissionFailed){
            write("The last mission failed!");
        }
        else{
            write("The last mission did not fail.");
        }
    }

    /**
     * Nominates a group of agents to go on a mission.
     * If the String does not correspond to a legitimate mission (<i>number</i> of distinct agents, in a String),
     * a default nomination of the first <i>number</i> agents (in alphabetical order) will be reported, as if this was what the agent nominated.
     *
     * @param number the number of agents to be sent on the mission
     * @return a String containing the names of all the agents in a mission, within 1sec
     */
    @Override
    public String do_Nominate(int number) {
        StringBuilder nominatedPlayers = new StringBuilder();
        int leader = players.indexOf(name);
        // Resistance Behaviour
        if(!spy){
            // If round 1 leader, choose the player to my right in hope to setup 3 in a row
            if(mission == 1){
                nominatedPlayers.append(name).append(players.charAt((leader + numPlayers - 1)%numPlayers));
            }
            else{
                TreeMap<Double, String> lowSuspicionTeam = new TreeMap<>();
                Double teamSuspicion;
                // For each spy combination
                for(String spyCombo : suspicion.keySet()){
                    teamSuspicion = 0.0;
                    for(String innerSpyCombo : suspicion.keySet()){
                        // Check if player from spyCombo resides withing innerSpyCombo
                        for(char c : spyCombo.toCharArray()){
                            // If resides in both, add suspicion, and move onto next innerSpyCombo
                            if(innerSpyCombo.contains(Character.toString(c))){
                                teamSuspicion += suspicion.get(innerSpyCombo);
                                break;
                            }
                        }
                    }
                    lowSuspicionTeam.put(teamSuspicion, spyCombo);
                }
            }

            // Nominate players from least suspicious to most suspicious
            nominatedPlayers.append(name)
        }
        else{
            // Government Behaviour
        }
        return nominatedPlayers.toString();
    }

    /**
     * Provides information of a given mission.
     *
     * @param leader  the leader who proposed the mission
     * @param mission a String containing the names of all the agents in the mission within 1sec
     **/
    @Override
    public void get_ProposedMission(String leader, String mission) {}

    /**
     * Gets an agents vote on the last reported mission
     *
     * @return true, if the agent votes for the mission, false, if they vote against it, within 1 sec
     */
    @Override
    public boolean do_Vote() {
        return false;
    }

    /**
     * Reports the votes for the previous mission
     *
     * @param yays the names of the agents who voted for the mission
     * @return within 100ms
     **/
    @Override
    public void get_Votes(String yays) {

    }

    /**
     * Reports the agents being sent on a mission.
     * Should be able to be infered from tell_ProposedMission and tell_Votes, but incldued for completeness.
     *
     * @param mission the Agents being sent on a mission
     * @return within 100ms
     **/
    @Override
    public void get_Mission(String mission) {

    }

    /**
     * Agent chooses to betray or not.
     *
     * @return true if agent betrays, false otherwise, within 1 sec
     **/
    @Override
    public boolean do_Betray() {
        return false;
    }

    /**
     * Reports the number of people who betrayed the mission
     *
     * @param traitors the number of people on the mission who chose to betray (0 for success, greater than 0 for failure)
     * @return within 100ms
     **/
    @Override
    public void get_Traitors(int traitors) {

    }

    /**
     * Optional method to accuse other Agents of being spies.
     * Default action should return the empty String.
     * Convention suggests that this method only return a non-empty string when the accuser is sure that the accused is a spy.
     * Of course convention can be ignored.
     *
     * @return a string containing the name of each accused agent, within 1 sec
     */
    @Override
    public String do_Accuse() {
        return null;
    }

    /**
     * Optional method to process an accusation.
     *
     * @param accuser the name of the agent making the accusation.
     * @param accused the names of the Agents being Accused, concatenated in a String.
     * @return within 100ms
     */
    @Override
    public void get_Accusation(String accuser, String accused) {

    }
}