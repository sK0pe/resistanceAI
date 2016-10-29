package s21469477;
import cits3001_2016s2.*;
import java.util.*;
import java.io.*;

/**
 * Created by Pradyumn on 3/10/2016.
 * Heuristic Agent is a Bayesian Update based agent that plays the game Resistance.
 * The Bayesian engine is particularly useful for the resistance members while exhibiting
 * fairly basic spy behaviour.
 *
 */
public class DimitriDevil implements Agent{
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
    private boolean statCheck = false;

    private static double RANDOM_PLAY = 0.1;
    private static double BETRAYAL_BLUNDER = 0.2;
    private static double RESISTANCE_YAY = 0.5;
    private static double VOTING_BLUNDER = 0.2;

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
    public DimitriDevil(){
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
            }
            else{
                //groups >= 2
                for (int j = i + 1; j < nPlayers; ++j) {
                    // groups of 2
                    if (groupSize == 2) {
                        playerCombo.setLength(0);
                        playerCombo.append(playerArr[i]).append(playerArr[j]);
                        suspicion.add(new PBlock(playerCombo.toString(), 0.0));
                    }
                    else{
                        // groups >= 3
                        for (int k = j + 1; k < nPlayers; ++k) {
                            if (groupSize == 3) {
                                playerCombo.setLength(0);
                                playerCombo.append(playerArr[i]).append(playerArr[j]).append(playerArr[k]);
                                suspicion.add(new PBlock(playerCombo.toString(), 0.0));
                            }
                            else{
                                // group >= 4
                                for (int l = k + 1; l < nPlayers; ++l) {
                                    if (groupSize == 4) {
                                        playerCombo.setLength(0);
                                        playerCombo.append(playerArr[i]).append(playerArr[j]).append(playerArr[k]).append(playerArr[l]);
                                        suspicion.add(new PBlock(playerCombo.toString(), 0.0));
                                    }
                                    else{
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
            this.spy = spies.contains(name);

            // Initialise Suspicion for all possible combinations of spies
            // Specify all resistance Members based on knowledge
            if(spy){
                this.resistanceMembers = characterRelativeComplement(players, spies);
            }
            // Initialise suspicion with equal probability for all combinations.
            getPlayerCombinations(suspicion, players, numSpies);
            for (PBlock spyCombo : suspicion) {
                spyCombo.suspicion = 1.0/(double)(nChooseK(numPlayers, numSpies));
            }
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
     * Secondary launcher when considering all teams with all players
     * * Helper function to determine a possible team's suspicion level based on current probabilities
     * @param allPossibleTeams      A container of all possible teams to consider, assumes all suspicion levels at 0.0.
     * @param suspicionArr          A container that holds all suspicion, in most cases will be the global suspicion array.
     *                              It does not include teams with "name" so 0.0 suspicion will be added when encountering
     *                              teams with self.
     *
     **/
    private void considerAllTeamSuspicion(ArrayList<PBlock> allPossibleTeams, ArrayList<PBlock> suspicionArr){
        considerAllTeamSuspicion(allPossibleTeams, suspicionArr, null);
    }

    /**
     * considerAllTeamsSuspicion
     *
     * Helper function to determine a possible team's suspicion level based on current probabilities
     * @param allPossibleTeams      A container of all possible teams to consider, assumes all suspicion levels at 0.0.
     * @param suspicionArr          A container that holds all suspicion, in most cases will be the global suspicion array.
     *                              It does not include teams with "name" so 0.0 suspicion will be added when encountering
     *                              teams with self.
     * @param personToExclude		A String which defines player to exclude from the Suspicion Array, if null, don't exclude
     * 								anyone
     *
     *
     */
    private void considerAllTeamSuspicion(ArrayList<PBlock> allPossibleTeams, ArrayList<PBlock> suspicionArr, String personToExclude){
        int possibleSpies;
        double normalisationFactor = 1.0;

        // Exclude this person
        if (personToExclude != null) {
            normalisationFactor = 0.0;
            for(PBlock spyCombo : suspicionArr) {
                if (!spyCombo.composition.contains(personToExclude)) {
                    normalisationFactor += spyCombo.suspicion;
                }
            }
        }

        // For all possible teams, check all combinations of spies
        for(PBlock consideredTeam : allPossibleTeams) {
            for (PBlock spyCombo : suspicionArr) {
                // Skip if excluding a player
                if (personToExclude != null && spyCombo.composition.contains(personToExclude)) {
                    continue;
                }

                possibleSpies = characterIntersection(consideredTeam.composition, spyCombo.composition).length();
                // Check if minimum Spies present
                if (possibleSpies >= minSpiesRequired) {
                    // Accumulate the suspicion and normalise
                    consideredTeam.setSuspicion(consideredTeam.suspicion + spyCombo.suspicion / normalisationFactor);
                }
            }
        }

        if(statCheck){
            for(PBlock consideredTeam : allPossibleTeams){
                write(consideredTeam.composition + " has cumulative suspicion of " + consideredTeam.suspicion);
            }
            write("\n");
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
            getPlayerCombinations(lowSuspicionTeam, playersExcludeSelf, number - 1);
        }
        else {
            if (minSpiesRequired == 1) {
                // Get all player combinations without spies to seed suspicion
                getPlayerCombinations(lowSuspicionTeam, resistanceMembers, number - 1);
            }
            else {
                // If we need another spy to go with us, consider all combinations with exactly
                // one other spy except ourselves.
                getPlayerCombinations(lowSuspicionTeam, resistanceMembers, number - 2);
                ArrayList<PBlock> lowSuspicionTeamWithSecondSpy = new ArrayList<>();
                for (PBlock block : lowSuspicionTeam) {
                    for (char spy : spies.toCharArray()) {
                        if (name.charAt(0) != spy) {
                            lowSuspicionTeamWithSecondSpy.add(new PBlock(block.composition + spy, 0.0));
                        }
                    }
                }
                lowSuspicionTeam = lowSuspicionTeamWithSecondSpy;
            }
        }

        // Add myself to each team being considered.
        ArrayList<PBlock> lowSuspicionTeamIncludingMe = new ArrayList<>();
        for (PBlock block : lowSuspicionTeam) {
            lowSuspicionTeamIncludingMe.add(new PBlock(block.composition + name, 0.0));
        }
        lowSuspicionTeam = lowSuspicionTeamIncludingMe;


        if (!spy){
            // Fill all possible mission teams with suspicion (from my own perspective)
            considerAllTeamSuspicion(lowSuspicionTeam, suspicion, name);
        }
        else{
            // Fill all possible mission teams with suspicion (from an external perspective)
            considerAllTeamSuspicion(lowSuspicionTeam, suspicion, null);
        }
        // Sort all possible mission teams, based on suspicion
        Collections.sort(lowSuspicionTeam);
        // Now have informed decision of providing best possible teams to go along with self as a leader (naive)
        // Return least likely team to have a spy on it plus self
        // As naive, works for Government spy and Resistance member
        if(statCheck){
            write(name + " is nominating the team " + lowSuspicionTeam.get(0).composition + ">>>>>>>>>>>>>>>");
        }
        return lowSuspicionTeam.get(0).composition;
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
        // Use this information to perform Bayesian update on suspicion array
        // P(spyCombo are spies | mission proposed by leader)
        Double prior;
        Double likelihood;
        Double unnormPos;
        Double totalProbability = 0.0;
        Double randomTeamPicking = 1.0/(double)(nChooseK(numPlayers, currProposedTeam.length()));
        ArrayList<Double> unnormPosteriors = new ArrayList<>(suspicion.size());
        String assumedSpiesInMission;

        // Iterate through all possibly spy combinations to find unnorm posterior probabilites and total probability
        for(PBlock spyCombo : suspicion){
            // Determine likelihood = P(mission proposed | spyCombo are spies)
            assumedSpiesInMission = characterIntersection(spyCombo.composition, currProposedTeam);
            // Inherit prior probability from suspicion array
            prior = spyCombo.suspicion;


            likelihood = RANDOM_PLAY * randomTeamPicking;
            if(minSpiesRequired > 1 && assumedSpiesInMission.contains(leader)) {
                // Spy needs to ensure the right number of spies for the mission.
                int numCombinations = nChooseK(numResistance, currProposedTeam.length() - minSpiesRequired) * nChooseK(numSpies - 1, minSpiesRequired - 1);
                // non random play
                likelihood += (1.0 - RANDOM_PLAY) * 1.0/(double)(numCombinations);
            }
            else {
                // Leader picks others to go with them completely at random.
                likelihood += (1.0 - RANDOM_PLAY) * 1.0/(double)(nChooseK(numPlayers - 1, currProposedTeam.length() - 1));
            }

            unnormPos = prior*likelihood;
            unnormPosteriors.add(unnormPos);
            totalProbability += unnormPos;
        }

        // Now that total Probability and unnormPosteriors are known, update prior with newly calculated posterior
        for(int i = 0; i < suspicion.size(); ++i){
            // Add posterior probability
            suspicion.get(i).suspicion = unnormPosteriors.get(i)/totalProbability;
        }

        if(statCheck){
            for(PBlock s : suspicion){
                if(name.equals("A")){
                    write(name + " says : Spyblock after get_ProposedMission is " + s.composition + " has suspicion level " + s.suspicion);
                }
            }
            write("\n");
        }

        if(!missionTeams.isEmpty()){
            missionTeams.clear();
        }

        // Get all possible player combinations.
        // Missions with self included will have lower suspicion as I'm definitely part of the
        // Resistance.
        getPlayerCombinations(missionTeams, players, currProposedTeam.length());
        // Populate suspicion level of others
        considerAllTeamSuspicion(missionTeams, suspicion, name);
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
        // Get average suspicion level
        Boolean belowMidRange = false;
        double midRange = (missionTeams.get(0).suspicion + missionTeams.get(missionTeams.size()-1).suspicion) /(double)numPlayers;
        for(PBlock m : missionTeams){
            if(m.composition.equals(currProposedTeam)){
                belowMidRange = (m.suspicion < midRange);
            }
        }

        // Spy behaviour, very simple
        if(spy){
            // Check how robust other Bayes engines are, should cause agent to be interpreted with less suspicion
            if(missionNum == 1 && numProposals % 2 == 0){
                return false;
            }
            // Not enough spies - reject the mission.
            if (characterIntersection(spies, currProposedTeam).length() < minSpiesRequired) {
                return false;
            }
            // Perfect situaton: # of spies = # betrayals required, so all spies will betray.
            else if (characterIntersection(spies, currProposedTeam).length() == minSpiesRequired && belowMidRange) {
                return true;
            }
            // Too many spies: we may need to worry about giving away spy identities via excess betrayals.
            else{
                // Mission failure would end the game: excess betrayals don't matter.
                if (numFailures == 2) {
                    return true;
                }
                // Only one betrayal required; the leader can betray safely.
                return (spies.contains(currLeader) && minSpiesRequired == 1 && belowMidRange);
            }
        }
        else{
            // missionTeams has been filled by the call to get_Proposed_Mission call earlier
            return belowMidRange;
        }
    }


    /**
     * Reports the votes for the previous mission
     *
     * @param yays the names of the agents who voted for the mission
     * return within 100ms
     **/
    @Override
    public void get_Votes(String yays) {
        // Need to do Bayesian updates to improve suspicion whether 0 or greater than 0
        Double prior;
        Double likelihood;
        Double unnormPos;
        Double totalProbability = 0.0;
        ArrayList<Double> unnormPosteriors = new ArrayList<>(suspicion.size());
        String nays = characterRelativeComplement(players, yays);
        String assumedSpiesinProposedTeam;

        for (PBlock spyCombo : suspicion){
            assumedSpiesinProposedTeam = characterIntersection(currProposedTeam, spyCombo.composition);
            int numSpiesInTeam = assumedSpiesinProposedTeam.length();
            // inherit last posterior
            prior = spyCombo.suspicion;

            // Default false vote for spies
            boolean spies_should_vote_yay = false;
            // Resistance vote yay with preset probability.
            // Resistance are alowed to change their vote split up by a factor of 0.5
            double resistance_yay_probability = RESISTANCE_YAY;

            // If it's the last proposal we assume the same likelihood regardless of the spy combo,
            // since spies and non-spies are equally likely to make this blunder.
            if (numProposals == 4) {
                spies_should_vote_yay = true;
                resistance_yay_probability = 1 - VOTING_BLUNDER;
            }
            else {
                // Spies won't vote for a team if there isn't enough spies
                if (numSpiesInTeam < minSpiesRequired) {
                    spies_should_vote_yay = false;
                }
                // Spies are expected to vote yes for when a team has the minRequired spies to fail the mission
                // Naive strategy
                else if (numSpiesInTeam == minSpiesRequired) {
                    spies_should_vote_yay = true;
                }
                else {
                    //  When numFailures == 2 desperate to get any mission in which they can win a game
                    if (numFailures == 2) {
                        spies_should_vote_yay = true;
                    }
                    // coordinated with non-leaders betraying when > 2 required
                    // if only 1 then the leader should betray as minimum evidence provided
                    else if (spyCombo.composition.contains(currLeader) && minSpiesRequired == 1) {
                        spies_should_vote_yay = true;
                    }
                }
            }

            // Likelihood that a Spy will vote yes when it should be voting yes
            double spy_yay_probability = spies_should_vote_yay ? 1.0 - VOTING_BLUNDER : VOTING_BLUNDER;
            // Assume that when spies are voting in a coordinated fashion that they are setting up to fail
            // a mission
            // Likelihood is product of the independent likelihoods for each individual voter.
            likelihood = 1.0;
            for ( Character yay : yays.toCharArray()) {
                if (spyCombo.composition.contains(yay + "")) {
                    likelihood *= spy_yay_probability;
                }
                else {
                    likelihood *= resistance_yay_probability;
                }
            }

            for (Character nay : nays.toCharArray()) {
                if (spyCombo.composition.contains(nay + "")) {
                    likelihood *= 1.0 - spy_yay_probability;
                }
                else {
                    likelihood *= 1.0 - resistance_yay_probability;
                }
            }

            // Unnorm posterior
            unnormPos = prior * likelihood;
            unnormPosteriors.add(unnormPos);
            totalProbability += unnormPos;
        }

        // Now that total Probability and unnormPosteriors are known
        for(int i = 0; i < suspicion.size(); ++i){
            // Add posterior probability
            suspicion.get(i).suspicion = unnormPosteriors.get(i)/totalProbability;
        }
        if(statCheck){
            for(PBlock s : suspicion){
                if(name.equals("A")){
                    write(name + " says : Spyblock after get_Votes is " + s.composition + " has suspicion level " + s.suspicion);
                }
            }
            write("\n");
        }
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
        int spiesOnMission = characterIntersection(spies, electedTeam).length();
        // As resistance member always want missions to succeed
        if(!spy){
            if(statCheck) write(name + " did not betray>>>>>>>>>>>>>>>");
            return false;
        }

        // Not enough spies - betrayal is pointless.
        if (spiesOnMission < minSpiesRequired) {
            if(statCheck) write(name + " did not betray>>>>>>>>>>>>>>>");
            return false;
        }
        // Perfect situaton: # of spies = # betrayals required, so all spies will betray.
        else if (spiesOnMission == minSpiesRequired) {
            if(statCheck) write(name + " did betray>>>>>>>>>>>>>>>");
            return true;
        }
        // Too many spies: we may need to worry about giving away spy identities via excess betrayals.
        else {
            // Mission failure would end the game: excess betrayals don't matter.
            if(missionNum == 1 && currProposedTeam.length() == 2){
                return false;
            }
            if (numFailures == 2) {
                if(statCheck) write(name + " did betray>>>>>>>>>>>>>>>");
                return true;
            }
            // The spies must fail all remaining missions to win the game, so betrayal is necessary.
            if (missionNum - numFailures == 3) {
                // Gives away more information but pressured to betray to win
                // In a better modelled agent may have differnt voting strategies
                if(statCheck) write(name + " did betray>>>>>>>>>>>>>>>");
                return true;
            }

            // Co-ordination solutions.

            // One betrayal required: leader betrays.
            if (currLeader.equals(name) && minSpiesRequired == 1) {
                if(statCheck) write(name + " did betray>>>>>>>>>>>>>>>");
                return true;
            }

            // # betrayals required = # of non-leader spies: non-leaders betray.
            if(!currLeader.equals(name) && (spiesOnMission - minSpiesRequired == 1) && spies.contains(currLeader)) {
                if(statCheck) write(name + " did betray>>>>>>>>>>>>>>>");
                return true;
            }
        }
        if(statCheck) write(name + " did not betray>>>>>>>>>>>>>>>");
        return false;
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
        // if(traitors > 0){

        for(PBlock spyCombo : suspicion){
            assumedSpiesInElectedTeam = characterIntersection(electedTeam, spyCombo.composition);
            int numSpiesInTeam = assumedSpiesInElectedTeam.length();
            // inherit last posterior
            prior = spyCombo.suspicion;

            // Safe to assume not to vote as voting to betrya gives up information
            boolean leader_should_betray = false;
            boolean non_leader_should_betray = false;

            if (numSpiesInTeam < minSpiesRequired) {
                // Presumably no reason to vote to betray
                // However it's possible for a spy to betary when should not
                leader_should_betray = false;
                non_leader_should_betray = false;
            }
            else if (numSpiesInTeam == minSpiesRequired) {
                // Every reason to betray
                leader_should_betray = true;
                non_leader_should_betray = true;
            }
            else {
                //  Can just win now as have enough voting strength to win
                //  EZ GAME
                if (numFailures == 2) {
                    leader_should_betray = true;
                    non_leader_should_betray = true;
                }
                // foced failures expected
                else if (missionNum - numFailures == 3) {
                    leader_should_betray = true;
                    non_leader_should_betray = true;
                }
                // coordinated with non-leaders betraying when > 2 required
                // if only 1 then the leader should betray
                else if (spyCombo.composition.contains(currLeader)) {
                    if (minSpiesRequired == 1) {
                        leader_should_betray = true;
                        non_leader_should_betray = false;
                    }
                    else if (assumedSpiesInElectedTeam.length() - minSpiesRequired == 1) {
                        leader_should_betray = false;
                        non_leader_should_betray = true;
                    }
                }
            }


            // spy combination does not include the current leader
            if (!spyCombo.composition.contains(currLeader)) {
                // probability that the non leader should betray = 95%, while the blunder case is 5% (when it does betray (unexpected)
                double p_betray = non_leader_should_betray ? 1.0 - BETRAYAL_BLUNDER : BETRAYAL_BLUNDER;
                // Binomial distribution (probability mass function)
                likelihood = Math.pow(p_betray, traitors) * Math.pow(1.0 - p_betray, numSpiesInTeam - traitors) * nChooseK(numSpiesInTeam, traitors);
            }
            else{
                double p_leader_betrays = leader_should_betray ? 1.0 - BETRAYAL_BLUNDER : BETRAYAL_BLUNDER;
                double p_non_leader_betrays = non_leader_should_betray ? 1.0 - BETRAYAL_BLUNDER : BETRAYAL_BLUNDER;

                likelihood = 0.0;
                // Leader betrays, (traitors-1) non-leaders betray, (num_spies-traitors) non-leaders do not betray.
                likelihood += p_leader_betrays * Math.pow(p_non_leader_betrays, traitors - 1) * Math.pow(1.0 - p_non_leader_betrays, numSpiesInTeam - traitors) * nChooseK(numSpiesInTeam-1, traitors-1);
                // Leader does not betray, (traitors) non-leaders betray, (num_spies-traitors-1) non-leaders do not betray.
                likelihood += (1 - p_leader_betrays) * Math.pow(p_non_leader_betrays, traitors) * Math.pow(1.0 - p_non_leader_betrays, numSpiesInTeam - traitors - 1) * nChooseK(numSpiesInTeam-1, traitors);
            }

            // Unnorm posterior
            unnormPos = prior * likelihood;
            unnormPosteriors.add(unnormPos);
            totalProbability += unnormPos;
        }

        // Now that total Probability and unnormPosteriors are known
        for(int i = 0; i < suspicion.size(); ++i){
            // Add posterior probability
            suspicion.get(i).suspicion = unnormPosteriors.get(i)/totalProbability;
        }
        if(statCheck){
            for(PBlock s : suspicion){
                if(name.equals("A")){
                    write(name + " says : Spyblock after get_Traitors is " + s.composition + " has suspicion level " + s.suspicion);
                }
            }
            write("\n");
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
        PBlock maxProbability = suspicion.get(0);
        for(PBlock spyCombo : suspicion){
            if(spyCombo.suspicion > maxProbability.suspicion && !spyCombo.composition.contains(name)){
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
