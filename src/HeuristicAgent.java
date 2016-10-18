import java.util.*;
import java.io.*;

/**
 * Created by Pradyumn on 3/10/2016.
 */
public class HeuristicAgent implements Agent{
    private String name;
    private String players = "";
    private String playersExcludeSelf = "";
    private String resistanceMembers = "";
    private String spies = "";
    private String currLeader = "";
    private String votedForMissionTeam = "";
    private String votedAgainstMissionTeam = "";
    private String electedTeam = "";
    private int numPlayers;
    private int numSpies;
    private int minSpiesRequired;
    private String currProposedTeam;
    // Am I a spy?
    private boolean spy;
    private int numProposals;
    // Missions failed up till current update
    private int failedMissions = 0;
    // What mission am I playing?
    private int missionNum = 0;
    private boolean lastMissionFailed = false;
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
     * getSortedString
     *
     * Helper function to sort Java strings.
     * O(nlgn)
     *
     * @param unsorted      The unsorted string input.
     * @return              A sorted String
     */
    private String getSortedString(String unsorted){
        char charArray[] = unsorted.toCharArray();
        Arrays.sort(charArray);
        return String.valueOf(charArray);
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
            // Specify all resistance Members based on knowledge
            for(Character s : spies.toCharArray()){
                if(s != name.charAt(0)){
                    removeSelf.deleteCharAt(removeSelf.indexOf(s.toString()));
                }
            }
            resistanceMembers = removeSelf.toString();


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
        this.missionNum = mission;
        // Zero the proposal fails from last mission
        this.numProposals = 0;
        // Determine how many spies are required to betray mission
        // i.e. only on games of player size 7 or higher and only on mission 4
        this.minSpiesRequired = (missionNum == 4 && numPlayers > 6) ? 2 : 1;
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
     * @param teamUnsorted      String which being checked for an intersection with spies.
     * @param spiesUnsorted     String which represents the spies, checking if these characters exist in team, if so keep for result.
     * @return found    String made up of character intersection between the 2 arrays
     */
    private String characterIntersection(String teamUnsorted, String spiesUnsorted){
        // Will usually encounter only sorted strings but double checking for robustness
        String team = getSortedString(spiesUnsorted);
        String spies = getSortedString(spiesUnsorted);
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
     * characterRelativeComplement
     *
     * Helper method to determine a set of characters A - set of characters B
     * @param baseSet       String which is being checked for relative complement
     * @param unwantedSet      String of unwanted characters in the case that they occur in baseSet
     * @return result       String of base set remaining after removing unwanted
     */
    private String characterRelativeComplement(String baseSet, String unwantedSet){
        String base = getSortedString(baseSet);
        String unwanted = getSortedString(unwantedSet);
        StringBuilder result = new StringBuilder();
        int i = 0;
        int j = 0;
        while(i < base.length() && j < unwanted.length()){
            while( base.charAt(i) < unwanted.charAt(j)){
                result.append(base.charAt(i++));
            }
            if(base.charAt(i) < unwanted.charAt(j)){
                ++i;
                ++j;
            }
        }
        while(i < base.length()){
            result.append(base.charAt(i));
            ++i;
        }
        return result.toString();
    }

    /**
     * considerAllTeamsSuspicion
     *
     * Helper function to determine a possible team's suspicion level based on current probabilities
     * @param allPossibleTeams      A container of all possible teams to consider, assumes all suspicion levels at 0.0.
     * @param suspicionArr          A container that holds all suspicion, in most cases will be the global suspcion array.
     *
     *
     */
    private void considerAllTeamSuspicion(ArrayList<PBlock> allPossibleTeams, ArrayList<PBlock> suspicionArr){
        int possibleSpies;
        for(PBlock consideredTeam : allPossibleTeams){
            for(PBlock spyCombo : suspicionArr){
                possibleSpies = characterIntersection(consideredTeam.composition, spyCombo.composition).length();
                // Check if minimum Spies present
                if(possibleSpies >= minSpiesRequired){
                    // Accumulate the suspicion
                    consideredTeam.setSuspicion(consideredTeam.suspicion + spyCombo.suspicion);
                }
            }
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
        // Find all possible combinations of size "number" - 1, excluding self, assume naively that include self every single time
        // Clear previous teams from prior mission
        if(!missionTeams.isEmpty()){
            missionTeams.clear();
        }
        // Initalise all possible mission team combinations and intialise to 0 suspicion
        if(!spy){
            // Only trust self
            getPlayerCombinations(missionTeams, playersExcludeSelf, number-1);
        }
        else{
            // Get all player combinations without spies to seed suspicion
            getPlayerCombinations(missionTeams, resistanceMembers, number - 1);
        }
        // Fill all possible mission teams with suspicion (excluding self, assume going on mission)
        considerAllTeamSuspicion(missionTeams, suspicion);
        // Sort all possible mission teams, based on suspicion
        Collections.sort(missionTeams);
        // Now have informed decision of providing best possible teams to go along with self as a leader (naive)
        // Return least likely team to have a spy on it plus self
        // As naive, works for Government spy and Resistance member
        return missionTeams.get(0).composition + name;
    }

    /**
     * Provides information of a given mission.
     *
     * @param leader  the leader who proposed the mission
     * @param mission a String containing the names of all the agents in the mission within 1sec
     **/
    @Override
    public void get_ProposedMission(String leader, String mission) {
        // With more advanced model have the mission propositions with high suspicion reflect an increase in suspicion
        // or a small variable in suspicion for all combinations in which leader is part of, possibly something that learns
        // when played against self.
        currLeader = leader;
        currProposedTeam = getSortedString(mission);
        // If I'm not the leader, check my suspicion for the team proposed
        // Make sure mission is alphabetically sorted as assigning a global variable
        if(!leader.equals(name)){
            if(!missionTeams.isEmpty()){
                missionTeams.clear();
            }
            // Resistance behaviour:
            // Get all possible player combinations, consider players excluding self but full number,
            // Missions with self included will have lower suspicion as I'm definitely part of the
            // Resistance.
            getPlayerCombinations(missionTeams, players, mission.length());
            // Populate suspicion level
            considerAllTeamSuspicion(missionTeams, suspicion);
            // Sort the ArrayList by suspicion
            Collections.sort(missionTeams);
        }
    }

    /**
     * Gets an agents vote on the last reported mission
     *
     * @return true, if the agent votes for the mission, false, if they vote against it, within 1 sec
     */
    @Override
    public boolean do_Vote() {
        numProposals++;
        // If I'm the leader I'm voting for my own mission
        if(currLeader.equals(name)){
            return true;
        }
        // If there have been 4 prior failed votes vote true regardless if Resistance as don't want to lose
        // If there have been 4 prior failed votes and I'm a spy, I give myself away if I vote false, therefore
        // same behaviour.
        if(numProposals == 4){
            return true;
        }
        else{
            // missionTeams has been filled by the call to get_Proposed_Mission call earlier
            for(int m = 0; m < missionTeams.size(); ++m){
                //  Find the relevant ranking (by suspicion) of the proposed team
                if(missionTeams.get(m).composition.equals(currProposedTeam)){
                    // Arrbitrary fractional cutoff instead of perfect answer, cannot determine how other agents act
                    // Possibly train this point
                    Double relevantRank = (double)m/(double)missionTeams.size();
                    Double cutoff = (double)numSpies/(double)numPlayers;
                    if( relevantRank <= cutoff){
                        return true;
                    }
                }
            }
        }
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
        // Record those who vote for and against missions to assist in Bayesian updates
        votedForMissionTeam = getSortedString(yays);
        votedAgainstMissionTeam = characterRelativeComplement(players, votedForMissionTeam);
    }

    /**
     * Reports the agents being sent on a mission.
     * Should be able to be inferred from tell_ProposedMission and tell_Votes, but included for completeness.
     *
     * @param mission the Agents being sent on a mission
     * @return within 100ms
     **/
    @Override
    public void get_Mission(String mission) {
        electedTeam = getSortedString(mission);
    }

    /**
     * Agent chooses to betray or not.
     *
     * @return true if agent betrays, false otherwise, within 1 sec
     **/
    @Override
    public boolean do_Betray() {
        // As resistance member always want missions to succeed
        if(!spy){
            return false;
        }
        else{
            // Government Spy behaviour
            int spiesOnMission = characterIntersection(electedTeam, spies).length();
            // If the mission team has only 2 people on the first mission return false to remove suspicion, may need to add a random
            // component to this
            // However would only be worthwhile if there was intelligence kept between games with the same agents, can't do that
            // so best to be safe earlier in the game
            if(electedTeam.length() == 2 && missionNum == 1){
                return false;
            }
            // Assume other agents will vote for in case that 2 or more agents are on mission, only need more than 1
            // vote on 4th mission in games of player size 7 and higher
            if(minSpiesRequired == 1 && spiesOnMission > 1){
                // maybe random seed this?
                return false;
            }
            return true;
        }
    }

    /**
     * Reports the number of people who betrayed the mission
     *
     * @param traitors the number of people on the mission who chose to betray (0 for success, greater than 0 for failure)
     * @return within 100ms
     **/
    @Override
    public void get_Traitors(int traitors) {
        // Need to do Bayesian updates to improve suspicion whether 0 or greater than 0
        Double prior = 1.0/(double)numPlayers;
        Double likelihood = 0.0;
        String spiesInElectedTeam;
        // Big assumption that traitors == spies and not resistance, hopefully implemented that Resistance
        // never betrays

        // If enouogh traitors to beray the mission
        if(traitors >= minSpiesRequired){
            for(PBlock spyCombo : suspicion){
                spiesInElectedTeam = characterIntersection(electedTeam, spyCombo.composition);
                // Assume that a leader spy won't select more than the required
                likelihood = 0.0;
                // If
                if(spiesInElectedTeam.contains(currLeader) && spiesInElectedTeam.length() > minSpiesRequired){
                    likelihood = 0.0;
                }
                if(spiesInElectedTeam.contains(currLeader) && spiesInElectedTeam.length() == minSpiesRequired){
                    likelihood = 1.0/(double)(numPlayers - numSpies);
                }
                else if(!spiesInElectedTeam.contains(currLeader) && spiesInElectedTeam.length() == minSpiesRequired){
                    likelihood = 1.0/(double)(playersExcludeSelf.length());
                }

                if(spiesInElectedTeam.length() == traitors && spiesInElectedTeam.length() <= minSpiesRequired){
                    if(spiesInElectedTeam.contains(currLeader)){

                    }
                    else if()
                }


                if(characterIntersection(electedTeam, spyCombo.composition).length() > minSpiesRequired){

                    if
                }
                if(spyCombo.composition.contains(currLeader) && electedTeam.contains(currLeader)){
                    likelihood = 1/(double)(numPlayers - numSpies);
                }
                else if(electedTeam.contains()){

                }

            }



        }
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
            // Always sorted
            this.composition = getSortedString(composition);
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
            // make sure all inputs are sorted
            composition = getSortedString(newComp);
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
