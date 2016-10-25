import java.util.*;
import java.io.*;

/**
 * Created by Pradyumn on 3/10/2016.
 * Heuristic Agent is a Bayesian Update based agent that plays the game Resistance.
 * The Bayesian engine is particularly useful for the resistance members while exhibiting
 * fairly basic spy behaviour.
 *
 */
public class HeuristicAgent implements Agent{
    private String name;
    private String players = "";
    private String playersExcludeSelf = "";
    private String resistanceMembers = "";
    private String spies = "";
    private String currLeader = "";
    private String electedTeam = "";
    private int numPlayers;
    private int numSpies;
    private int numResistance;
    private int minSpiesRequired;
    private int numFailures;
    private String currProposedTeam;
    // Am I a spy?
    private boolean spy;
    private int numProposals;
    // What mission am I playing?
    private int missionNum = 0;
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
     * nChooseK
     *
     * Probability function helper, determines how many unique options of size k can be derived from something size n
     * @param n     The integer representing the pool size from which options are being extracted
     * @param k     The integer size k which determines the size of the combinations being looked for inside of n
     * @return      Integer representing the number of unique options possible
     */
    private int nChooseK(int n, int k){
        if( k < 0 || k > n){
            return 0;
        }
        if(k > n/2){
            k = n - k;
        }
        int answer = 1;
        for(int i = 1; i <= k; ++i){
            answer *= (n + 1 - i);
            answer /= i;
        }
        return answer;
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
     * return within 100ms
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
            this.playersExcludeSelf = characterRelativeComplement(players, name);
            // Spy string provided from Game
            this.spies = spies;
            this.numSpies = spies.length();
            this.numResistance = numPlayers - numSpies;

            // If spy string contains my name, I'm a spy
            if(spies.contains(name)){
                this.spy = true;
            }
            else{
                this.spy = false;
            }

            // Initialise Suspicion for all possible combinations of spies
            // Specify all resistance Members based on knowledge
            if(spy){
                this.resistanceMembers = characterRelativeComplement(players, spies);
            }
            // Initialise suspicion to 0.0
            getPlayerCombinations(this.suspicion, this.playersExcludeSelf, numSpies);
        }

        // Update mission number every round
        this.missionNum = mission;
        // Zero the proposal fails from last mission
        this.numProposals = 0;
        // Number of failures
        this.numFailures = failures;
        // Determine how many spies are required to betray mission
        // i.e. only on games of player size 7 or higher and only on mission 4
        this.minSpiesRequired = (missionNum == 4 && numPlayers > 6) ? 2 : 1;
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
        String team = getSortedString(teamUnsorted);
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
        return found.toString();
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
            if(base.charAt(i) == unwanted.charAt(j)){
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
     * @param suspicionArr          A container that holds all suspicion, in most cases will be the global suspicion array.
     *                              It does not include teams with "name" so 0.0 suspicion will be added when encountering
     *                              teams with self.
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
        ArrayList<PBlock> lowSuspicionTeam = new ArrayList<>();
        // Initalise all possible mission team combinations and intialise to 0 suspicion
        if(!spy){
            // Only trust self
            getPlayerCombinations(lowSuspicionTeam, playersExcludeSelf, number-1);
        }
        else{
            // Get all player combinations without spies to seed suspicion
            getPlayerCombinations(lowSuspicionTeam, resistanceMembers, number - 1);
        }
        // Fill all possible mission teams with suspicion (excluding self, assume going on mission)
        considerAllTeamSuspicion(lowSuspicionTeam, suspicion);
        // Sort all possible mission teams, based on suspicion
        Collections.sort(lowSuspicionTeam);
        // Now have informed decision of providing best possible teams to go along with self as a leader (naive)
        // Return least likely team to have a spy on it plus self
        // As naive, works for Government spy and Resistance member
        return lowSuspicionTeam.get(0).composition + name;
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

        // Use this information to perform Bayesian update on suspicion array
        // P(spyCombo are spies | mission proposed by leader)
        Double prior;
        Double likelihood = 0.0;
        Double unnormPos;
        Double totalProbability = 0.0;
        ArrayList<Double> unnormPosteriors = new ArrayList<>(suspicion.size());
        String assumedSpiesInMission;

        // Iterate through all possibly spy combinations to find unnorm posterior probabilites and total probability
        for(PBlock spyCombo : suspicion){
            // Determine likelihood = P(mission proposed | spyCombo are spies)
            assumedSpiesInMission = characterIntersection(spyCombo.composition, currProposedTeam);
            if(missionNum > 1){
                // Inherit prior probability from suspicion array
                prior = spyCombo.suspicion;
            }
            else{
                prior = 1.0/(double)(nChooseK(numPlayers, numSpies));
            }

            // Hard coded likelihood if leader, can choose any team including spies or non spies, except missions with 2
            // spies required
            if(minSpiesRequired == 2 && assumedSpiesInMission.contains(leader)){
                likelihood = 1.0/(double)(nChooseK(numResistance, currProposedTeam.length() - minSpiesRequired));
            }
            else{
                likelihood = 1.0/(double)(nChooseK(numPlayers - 1, currProposedTeam.length() - 1));
            }

//            if(assumedSpiesInMission.contains(leader)){
//                // Assume spy leader wants resistance members to fill all but required slots
//                likelihood = 1.0/(double)(nChooseK(numResistance, currProposedTeam.length() - minSpiesRequired));
//            }
//            else{
//                likelihood = 1.0/(double)(nChooseK(numPlayers - 1, currProposedTeam.length() - 1));
//            }

            unnormPos = prior*likelihood;
            unnormPosteriors.add(unnormPos);
            totalProbability += unnormPos;
        }

        // Now that total Probability and unnormPosteriors are known, update prior with newly calculated posterior
        for(int i = 0; i < suspicion.size(); ++i){
            // This if check is a sanity check that does not remove prior history, primarily aimed at not losing
            // Bayesian update history when encountering multiple spies on a mission (more than required to succeed)
            if(unnormPosteriors.get(i) != 0.0){
                // Add posterior probability
                suspicion.get(i).suspicion = unnormPosteriors.get(i)/totalProbability;
            }
        }


        write(name + "suspicions = ");
        for(PBlock s : suspicion){
            write("Spyblock after get_ProposedMission is " + s.composition + " has suspicion level " + s.suspicion);
        }
        write("\n");

        if(!missionTeams.isEmpty()){
            missionTeams.clear();
        }

        // Get all possible player combinations.
        // Missions with self included will have lower suspicion as I'm definitely part of the
        // Resistance.
        getPlayerCombinations(missionTeams, players, mission.length());
        // Populate suspicion level
        considerAllTeamSuspicion(missionTeams, suspicion);
        // Sort the ArrayList by suspicion
        Collections.sort(missionTeams);
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
        // If there have been 4 prior failed votes vote true regardless if Resistance as don't want to lose
        // If there have been 4 prior failed votes and I'm a spy, I give myself away if I vote false, therefore
        // same behaviour.
        if(currLeader.equals(name) || numProposals == 4){
            return true;
        }
        // Spy behaviour, very simple
        // Could be married into the suspicion check so that spies don't vote for suspicious missions
        if(spy){
            String spyTeam = characterIntersection(spies, currProposedTeam);
            if(spyTeam.length() >= minSpiesRequired){
                if(spyTeam.length() > minSpiesRequired && characterIntersection(spyTeam, currLeader).length() == 0){
                    return false;
                }
                return true;
            }
            return false;
        }
        else{
            // missionTeams has been filled by the call to get_Proposed_Mission call earlier
            for(int m = 0; m < missionTeams.size(); ++m){
                //  Find the relevant ranking (by suspicion) of the proposed team
                if(missionTeams.get(m).composition.equals(currProposedTeam)){
                    // Arbitrary fractional cutoff instead of perfect answer, cannot determine how other agents act
                    // Possibly train this point
                    Double relevantRank = (double)m/(double)missionTeams.size();
                    Double cutoff = (double)numSpies/(double)numPlayers;
                    if( relevantRank <= cutoff || missionTeams.get(m).suspicion.equals(missionTeams.get(0).suspicion)){
                        return true;
                    }
                    else{
                        return false;
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
     * return within 100ms
     **/
    @Override
    public void get_Votes(String yays) {
        // Record those who vote for and against missions to assist in Bayesian updates
        String votedForMissionTeam = getSortedString(yays);
        String nays = characterRelativeComplement(players, votedForMissionTeam);
        String assumedSpiesInNays;

        // Check if naive spy gave away that they are a spy
        // Can't do a meaningful Bayesian update on the last opportunity to vote for mission
        // If there is a differential, it gives away who is a spy
        if(numProposals == 4){
            if(nays.length() > 0){
                for(PBlock spyCombo : suspicion){
                    assumedSpiesInNays = characterIntersection(spyCombo.composition, nays);
                    if(assumedSpiesInNays.length() > 0){
                        spyCombo.suspicion = assumedSpiesInNays.length()/(double)numSpies;
                    }
                }

                write(name + "suspicions = ");
                for(PBlock s : suspicion){
                    write("Spyblock after get_Votes on turn 4 is " + s.composition + " has suspicion level " + s.suspicion);
                }
                write("\n");
            }
            return;
        }

        // Use this information to perform Bayesian update on suspicion array for
        // P(spyCombo are spies | mission proposed by leader)
        Double prior;
        Double likelihood;
        Double unnormPos;
        Double totalProbability = 0.0;
        ArrayList<Double> unnormPosteriors = new ArrayList<>(suspicion.size());

        // Iterate through all possibly spy combinations to find unnorm posterior probabilites and total probability
        for(PBlock spyCombo : suspicion){
            // Determine likelihood = P(mission proposed | spyCombo are spies)
            assumedSpiesInNays = characterIntersection(spyCombo.composition, nays);
            prior = spyCombo.suspicion;
            // voting against a mission is always suspicious assuming good missions should be offered
            if(assumedSpiesInNays.length() > 0){
                likelihood = 1.0/(1.0 + (double)nChooseK(currProposedTeam.length(), 2));
            }
            else{
                likelihood = 1.0/(double)nChooseK(numPlayers - numSpies, currProposedTeam.length() - 1);
            }
            unnormPos = prior*likelihood;
            unnormPosteriors.add(unnormPos);
            totalProbability += unnormPos;
        }
        // Now that total Probability and unnormPosteriors are known, update prior with newly calculated posterior
        for(int i = 0; i < suspicion.size(); ++i){
            suspicion.get(i).suspicion = unnormPosteriors.get(i)/totalProbability;
        }

        write(name + "suspicions = ");
        for(PBlock s : suspicion){
            write("Spyblock after get_Votes is " + s.composition + " has suspicion level " + s.suspicion);
        }
        write("\n");
    }

    /**
     * Reports the agents being sent on a mission.
     * Should be able to be inferred from tell_ProposedMission and tell_Votes, but included for completeness.
     *
     * @param mission the Agents being sent on a mission
     * return within 100ms
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
            if(spiesOnMission > minSpiesRequired && numFailures < 2){
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
     * return within 100ms
     **/
    @Override
    public void get_Traitors(int traitors) {
        // Need to do Bayesian updates to improve suspicion whether 0 or greater than 0
        Double prior;
        Double likelihood;
        Double unnormPos;
        Double totalProbability = 0.0;
        ArrayList<Double> unnormPosteriors = new ArrayList<>(suspicion.size());

        String assumedSpiesInElectedTeam;
        // Big assumption that traitors == spies and not resistance, hopefully implemented that Resistance
        // never betrays

        // If single traitor exists need to add suspicion for presence in team, regardless of win or not
        // Doesn't win in Misison 4 of 7 player and higher games
        if(traitors > 0){
            for(PBlock spyCombo : suspicion){
                assumedSpiesInElectedTeam = characterIntersection(electedTeam, spyCombo.composition);
                likelihood = 0.0;
                // inherit last posterior
                prior = spyCombo.suspicion;

                // Unintentional betrayal assumption is when the leader unknowingly picks a spy for the mission
                // Intentional betrayal (leader is spy)

                // Assume that a leader spy won't select more than the required and picks self to go on mission
                // Keep greater than because don't know other agent code
                // ---Mission failed---
                if(assumedSpiesInElectedTeam.length() == minSpiesRequired){
                    // If Leader of mission is in the spycombo being examined
                    if(assumedSpiesInElectedTeam.contains(currLeader)){
                        // Assume spy leader specifically picks resistance players and number of spies for task
                        likelihood = 1.0/(double)(minSpiesRequired * nChooseK(numPlayers - numSpies, electedTeam.length() - 1));
                    }
                    // If leader NOT in spycombo but spycombo still causes mission failure
                    else{
                        // unintentional pick
                        likelihood = 1.0/(double)(nChooseK(numPlayers-1, electedTeam.length() - 1));
                    }
                }
                // Mission --Mission accidentally won, spies voted 1 betrayal when needed 2--
                else if(assumedSpiesInElectedTeam.length() < minSpiesRequired){
                    // If leader, the leader knows to pick more than 1 spy or allow mission to succeed thus not
                    // likely to be in this situation, however can occur if 1 spy betrays, the other does not or if
                    // one spy is on mission by it's self and naively betrays in which case it is an unintentional pick
                    likelihood = 1.0/(double)(nChooseK(numPlayers-1, electedTeam.length() - 1));
                }

                // Unnorm posterior
                unnormPos = prior * likelihood;
                unnormPosteriors.add(unnormPos);
                totalProbability += unnormPos;
            }

            // Now that total Probability and unnormPosteriors are known
            for(int i = 0; i < suspicion.size(); ++i){
                // Add posterior probability
                // Do not let future priors be reset to 0.0 because not handling case where 2 spies on the same team
                if(unnormPosteriors.get(i) != 0.0){
                    suspicion.get(i).suspicion = unnormPosteriors.get(i)/totalProbability;
                }
            }
        }
        write(name + "suspicions = ");
        for(PBlock s : suspicion){
            write("Spyblock after get_Traitors is " + s.composition + " has suspicion level " + s.suspicion);
        }
        write("\n");
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
        PBlock maxProbability = suspicion.get(0);
        for(PBlock spyCombo : suspicion){
            if(spyCombo.suspicion > maxProbability.suspicion){
                maxProbability = spyCombo;
            }
        }
        return maxProbability.composition;
    }

    /**
     * Optional method to process an accusation.
     *
     * @param accuser the name of the agent making the accusation.
     * @param accused the names of the Agents being Accused, concatenated in a String.
     * return within 100ms
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
        private PBlock(String composition, Double suspicion){
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

        private void setSuspicion(Double newSuspicion){
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
