import java.util.*;
import java.io.*;

/**
 * Created by Pradyumn on 3/10/2016.
 */
public class HeuristicAgent implements Agent{
    private String name;
    private String players = "";
    private String playersExcludeSelf = "";
    private String spies = "";
    private int numPlayers;
    private int numSpies;
    private int nominationFailed = 0;
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
    private ArrayList<PBlock> suspicion = new ArrayList<>();
    private ArrayList<PBlock> missionTeams = new ArrayList<>();
    //private int suspicionSize[] = {10, 15, 35, 56, 84, 210};
    //private int suspicionDiscountSelf[] = {6, 10, 20, 35, 56, 126};

    // Printing out
    public HeuristicAgent(){
        this.display = System.out;
    }
    private void write(String s){
        display.println(s);
    }


    /**
     * getPlayerCombinations
     *
     * Helper function to build all combinations of players of specified
     * size along with initialising the combination's suspicion level.
     */
    private void getPlayerCombinations(ArrayList<PBlock> suspicion, String relevantPlayers, int groupSize){
        int nPlayers = relevantPlayers.length();
        if(groupSize < 1 || groupSize > 5 || groupSize > nPlayers || nPlayers > numPlayers) throw new RuntimeException("Choosing " + groupSize + " players from " + relevantPlayers + " total is inappropriate");
        //Sort the players string as not guaranteed
        char playerArr[] = relevantPlayers.toUpperCase().toCharArray();
        Arrays.sort(playerArr);

        StringBuilder playerCombo = new StringBuilder();
        // Combinations are at a minimum size 2 and maximum size 5
        for(int i = 0; i < nPlayers; ++i) {
            // group of 1
            if (groupSize == 1) {
                playerCombo.setLength(0);
                playerCombo.append(playerArr[i]);
                suspicion.add(new PBlock(playerCombo.toString(), 0.0));
            } else {
                //groups >= 2
                for (int j = i + 1; j < nPlayers; ++j) {
                    // groups of 2
                    if (groupSize == 2) {
                        playerCombo.setLength(0);
                        playerCombo.append(playerArr[i]).append(playerArr[j]);
                        suspicion.add(new PBlock(playerCombo.toString(), 0.0));
                    } else {
                        // groups >= 3
                        for (int k = j + 1; k < nPlayers; ++k) {
                            if (groupSize == 3) {
                                playerCombo.setLength(0);
                                playerCombo.append(playerArr[i]).append(playerArr[j]).append(playerArr[k]);
                                suspicion.add(new PBlock(playerCombo.toString(), 0.0));
                            } else {
                                // group >= 4
                                for (int l = k + 1; l < nPlayers; ++l) {
                                    if (groupSize == 4) {
                                        playerCombo.setLength(0);
                                        playerCombo.append(playerArr[i]).append(playerArr[j]).append(playerArr[k]).append(playerArr[l]);
                                        suspicion.add(new PBlock(playerCombo.toString(), 0.0));
                                    } else {
                                        // group of 5
                                        for (int m = l + 1; m < nPlayers; ++m) {
                                            playerCombo.setLength(0);
                                            playerCombo.append(playerArr[i]).append(playerArr[j]).append(playerArr[k]).append(playerArr[l]).append(playerArr[m]);
                                            suspicion.add(new PBlock(playerCombo.toString(), 0.0));
                                        }
                                    }
                                }
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
            // Make string of players excluding self
            StringBuilder removeSelf = new StringBuilder(players);
            removeSelf.deleteCharAt(removeSelf.indexOf(name));
            this.playersExcludeSelf = removeSelf.toString();
            // Spy string provided from Game
            this.spies = spies;
            this.numSpies = spies.length();
            // If spy string contains my name, I'm a spy
            this.spy = spies.contains(name);
            // Initialise Suspicion for all possible combinations of spies
            if(spy){
                // As a spy, keep track of own suspicion and everyone else
                getPlayerCombinations(suspicion, players, numSpies);
            }
            else {
                // As Government agent ignore own suspicion, only interested in finding spies
                getPlayerCombinations(suspicion, playersExcludeSelf, numSpies);
            }
        }

        // Update mission number every round
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
            write("The last mission succeeded.");
        }
    }

    /**
     * characterIntersection
     *
     * Helper method for do_nominate and
     * Finds the intersection of 2 character arrays, returning the character intersection as a string
     * @param team      character array which being checked for an intersection with spies.
     * @param spies     character array which represents the spies, checking if these characters exist in team, if so keep for result.
     * @return found    String made up of character intersection between the 2 arrays
     */
    private int characterIntersection(String team, String spies){
//        char team[] = teamString.toCharArray();
//        char spies[] = spyString.toCharArray();
//        Arrays.sort(team);
//        Arrays.sort(spies);
        int i = 0, j = 0;
        StringBuilder found = new StringBuilder();
        while(i < team.length() && j < spies.length()){
            if(team.charAt(i) < spies.charAt(j)){
                ++i;
            }
            else{
                if(!(spies.charAt(j) < team.charAt(i))){
                    found.append(team.charAt(i));
                    ++i;
                }
                ++j;
            }
        }
        return found.length();
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
        // Find all possible combinations of size "number" - 1, excluding self, assume naively that include self every single time
        // If first round create all combinations
        if(nominationFailed == 0){
            // Clear previous teams from prior mission
            if(mission > 1){
                missionTeams.clear();
            }
            // First time run for this round, initalise all possible mission team combinations
            // Check if need 2 spies to fail mission, numPlayers 7 and higher, only on mission 4
            int minSpiesRequired = (mission == 4 && numPlayers > 6) ? 2 : 1;
            int possibleSpies;
            getPlayerCombinations(missionTeams, playersExcludeSelf, number-1);
            for(PBlock consideredTeam : missionTeams){
                for(PBlock spyCombo : suspicion){
                    possibleSpies = characterIntersection(consideredTeam.composition, spyCombo.composition);
                    if(possibleSpies >= minSpiesRequired){
                        // Accumulate the suspicion
                        consideredTeam.setSuspicion(consideredTeam.suspicion + spyCombo.suspicion);
                    }
                }
            }
            // Now have informed decision of providing best possible teams to go along with me as a leader

        }
        else{
            // Last team nominated failed try next least likely
        }





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


    /**
     * PBlock private class
     *
     * Holds the data of likelihood of the the group made up by the string present
     */
    private class PBlock implements Comparable<PBlock>{
        private String composition;
        private Double suspicion;

        // PBLock Constructor
        public PBlock(String composition, Double suspicion){
            char compArray[] = composition.toCharArray();
            Arrays.sort(compArray);
            // Always sorted
            this.composition = String.valueOf(compArray);
            this.suspicion = suspicion;
        }

        // Hashing for hash structures
        @Override
        public int hashCode(){
            int hashComposition = (composition != null) ? composition.hashCode() : 0;
            int hashSuspicion = (suspicion != null) ? suspicion.hashCode() : 0;
            return (hashComposition + hashSuspicion)*hashSuspicion + hashComposition;
        }

        // Boolean equals
        public boolean equals(Object other){
            if(other instanceof PBlock){
                PBlock otherBlock = (PBlock)other;
                // If strings are equal
                if(this.composition != null && otherBlock.composition != null && this.composition.equals(otherBlock.composition)) {
                    // return whether suspicion is equal
                    return Objects.equals(this.suspicion, otherBlock.suspicion);
                }
            }
            return false;
        }

        // ToString
        public String toString(){
            return "(" + composition + ", " + suspicion + ")";
        }

        // getters
        public String getComposition(){
            return composition;
        }

        public Double getSuspicion(){
            return suspicion;
        }

        // setters
        public void setComposition(String newComp){
            // make sure is sorted
            char compArray[] = newComp.toCharArray();
            Arrays.sort(compArray);
            composition = String.valueOf(compArray);
        }

        public void setSuspicion(Double newSuspicion){
            suspicion = newSuspicion;
        }


        @Override
        public int compareTo(PBlock o) {
            if(this.suspicion > o.suspicion){
                return 1;
            }
            else if(this.suspicion < o.suspicion){
                return -1;
            }
            return this.composition.compareTo(o.composition);
        }
    }
}
