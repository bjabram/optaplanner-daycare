package bracketplanner.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;
import bracketplanner.domain.Bracket;
import bracketplanner.domain.Seeding;
import bracketplanner.domain.Site;
import bracketplanner.domain.Team;
import bracketplanner.util.BracketPlannerUtil;

/**
 * @author babram
 * 
 */
public class BracketGenerator {
    static final Logger log = LoggerFactory.getLogger(BracketGenerator.class);
    static final String TEAMS_FILE_URL = "teams-2014.csv";
    static final String RANKINGS_FILE_URL = "rankings-2014.csv";
    static final String SITES_FILE_URL = "sites-2014.csv";

    public static Bracket generateBracket() {

        Bracket unsolvedBracket = new Bracket();

        // Import list of teams
        Map<String, Team> unrankedTeamMap = new HashMap<String, Team>();
        try {
            CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(BracketGenerator.class.getClassLoader()
                    .getResourceAsStream((TEAMS_FILE_URL)))), ',', '"', 1);
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                String name = nextLine[4];
                String abbreviation = nextLine[5];
                String conference = nextLine[1];
                double latitude = Double.parseDouble(nextLine[10]);
                double longitude = Double.parseDouble(nextLine[11]);
                unrankedTeamMap.put(abbreviation, new Team(-1, name, abbreviation, conference, latitude, longitude));
            }
            reader.close();
        } catch (IOException ex) {
            throw new RuntimeException("Could not read CSV file");
        }

        Map<String, Integer> teamsInConference = new HashMap<String, Integer>();
        List<Team> rankedTeamList = new ArrayList<Team>();
        try {
            CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(BracketGenerator.class.getClassLoader()
                    .getResourceAsStream((RANKINGS_FILE_URL)))), ',', '"', 1);
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                int rank = Integer.parseInt(nextLine[0]);
                String abbreviation = nextLine[2];
                Team rankedTeam = unrankedTeamMap.get(abbreviation);

                // calculate ranking within conference
                int rankInConference = 1;
                String conference = rankedTeam.getConference();
                if (teamsInConference.containsKey(conference)) {
                    rankInConference = teamsInConference.get(conference) + 1;
                }
                teamsInConference.put(conference, rankInConference);
                rankedTeam.setRankInConference(rankInConference);

                rankedTeam.setRank(rank);
                rankedTeamList.add(rankedTeam);
            }
            reader.close();
        } catch (IOException ex) {
            throw new RuntimeException("Could not read CSV file");
        }

        // Create seeds
        List<Integer> seedList = new ArrayList<Integer>(BracketPlannerUtil.NUM_TEAMS_PER_REGION);
        for (int i = 1; i <= BracketPlannerUtil.NUM_TEAMS_PER_REGION; i++) {
            seedList.add(i);
        }

        // Import list of game sites
        List<Site> podSiteList = new ArrayList<Site>();
        List<Site> regionalSiteList = new ArrayList<Site>();

        try {
            CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(BracketGenerator.class.getClassLoader()
                    .getResourceAsStream((SITES_FILE_URL)))), ',', '"', 1);
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                int round = Integer.parseInt(nextLine[0]);
                int venueId = Integer.parseInt(nextLine[1]);
                String name = nextLine[2];
                String location = nextLine[3];
                String hostTeamname = nextLine[4];
                double latitude = Double.parseDouble(nextLine[5]);
                double longitude = Double.parseDouble(nextLine[6]);
                Site site = new Site(name, location, round, hostTeamname, venueId, latitude, longitude);
                if (round == 1)
                    podSiteList.add(site);
                else if (round == 2)
                    regionalSiteList.add(site);
            }
            reader.close();
        } catch (IOException ex) {
            throw new RuntimeException("Could not read CSV file");
        }

        // Create initial list of seedings
        List<Seeding> seedingList = new ArrayList<Seeding>(BracketPlannerUtil.NUM_TEAMS);
        for (int i = 0; i < BracketPlannerUtil.NUM_TEAMS; i++) {
            Seeding seeding = new Seeding();
            seeding.setSeed(seedList.get(BracketPlannerUtil.getSeedIndex(i)));
            seeding.setPod(BracketPlannerUtil.getPodIndex(i) + 1);
            seeding.setRegionalSite(regionalSiteList.get(BracketPlannerUtil.getRegionIndex(i)));
            seedingList.add(seeding);
        }

        unsolvedBracket.setTeams(rankedTeamList);
        unsolvedBracket.setSeeds(seedList);
        unsolvedBracket.setSites(podSiteList);
        unsolvedBracket.setSeedings(seedingList);

        return unsolvedBracket;
    }
}
