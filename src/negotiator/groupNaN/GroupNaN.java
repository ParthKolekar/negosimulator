package negotiator.groupNaN;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import agents.anac.y2013.MetaAgent.portfolio.thenegotiatorreloaded.TheNegotiatorReloaded_Offering.DiscountTypes;

import negotiator.AgentID;
import negotiator.Bid;
import negotiator.Deadline;
import negotiator.Domain;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.issue.Issue;
import negotiator.issue.Value;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.session.TimeLineInfo;
import negotiator.utility.AbstractUtilitySpace;

public class GroupNaN extends AbstractNegotiationParty {
	protected AgentID partyId = new AgentID("Group NaN");
	protected double acceptThreshold = 0.1;
	protected double timeThreshold = 0.6;
	private Bid bestBid;
	private double reservationValue;
	private double bestUtil;
	private double worstUtil;
	private double timeLimit;
	private HashMap<AgentID, Action> latestBids;
	private HashMap<AgentID, Action> firstBids;
	private Date startTime;
	private Domain domain;
	ArrayList<Bid> initialCrossOverResult = null;
	int rounds;

	public GroupNaN() {
		
	}

	Comparator<Bid> comparator = new Comparator<Bid>() {

		@Override
		public int compare(Bid o1, Bid o2) {
			return Double.compare(getUtility(o2),getUtility(o1));
		}
	};
	
	public void init (AbstractUtilitySpace utilSpace, Deadline dl, TimeLineInfo tl, long randomSeed, AgentID agentId) {
		super.init(utilSpace, dl, tl, randomSeed, agentId);
		this.timeLimit = dl.getTimeOrDefaultTimeout();
		this.latestBids = new HashMap<AgentID, Action>();
		this.firstBids = new HashMap<AgentID, Action>();
		this.domain = utilSpace.getDomain();
		try {
			this.bestBid = utilSpace.getMaxUtilityBid();
			this.worstUtil = getUtility(utilSpace.getMinUtilityBid());
			this.reservationValue= utilSpace.getReservationValueUndiscounted();
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.bestUtil = getUtility(this.bestBid);
		new ArrayList<Bid>();
		this.startTime = new Date();
		this.rounds=0;
	}

	
	public void receiveMessage(AgentID sender, Action action) {
		this.latestBids.put(sender, action);
		if (this.firstBids.get(sender) == null) {
			this.firstBids.put(sender, action);
		}
	}
	
	private boolean acceptable() {
		boolean result = false;
		for(Action action: this.latestBids.values())
			result &= (action.getClass()==Accept.class) || (this.bestUtil - getUtility(Action.getBidFromAction(action)) <= this.acceptThreshold);
		return result;
	}

	private Bid getRandomBestBid(List<Bid> list) {		
		double totalWeight = 0.0d;
		for(Bid bid : list) 
			totalWeight += getUtility(bid);
		
		double random = Math.random() * totalWeight;
		for (int i=0;i<list.size();i++) {
		    random -= getUtility(list.get(i));
		    if (random <= 0.0d)
		        return list.get(i);
		}
		return this.bestBid;
	}
	
	private ArrayList<Bid> crossover(Bid bid1, Bid bid2) {
		ArrayList<Bid> result = new ArrayList<Bid>();
		for(Issue issue : this.domain.getIssues()) {
			int inum = issue.getNumber();
			HashMap<Integer, Value> hiv1 = bid1.getValues();
			HashMap<Integer, Value> hiv2 = bid2.getValues();
			Value v1 = hiv1.get(inum),v2 = hiv2.get(inum);
			hiv1.put(inum,v2);
			hiv2.put(inum,v1);
			try {
				result.add(new Bid(this.domain,hiv1));
				result.add(new Bid(this.domain,hiv2));
			} catch (Exception e) {
				result.add(this.bestBid);
				result.add(this.bestBid);
				e.printStackTrace();
			}
		}
			
		return result;
	}
	private Bid getGeneticBestBid(double timeDone) {
		
		if (initialCrossOverResult == null && this.firstBids.values() != null) {
			ArrayList<Bid> initialCrossOver = new ArrayList<Bid>();
			initialCrossOverResult = new ArrayList<Bid>();
			initialCrossOver.add(this.bestBid);
			for (Action action : this.firstBids.values()) {
				if (Action.getBidFromAction(action) != null) {
					initialCrossOver.add(Action.getBidFromAction(action));
				}
			}
			
			for (int i = 0; i < initialCrossOver.size() - 1 ; i++) {
				for (int j = i + 1 ; j < initialCrossOver.size() ; j++) {
					initialCrossOver.get(i);
					initialCrossOver.get(j);
					initialCrossOverResult.addAll(crossover(initialCrossOver.get(i), initialCrossOver.get(j)));
				}
			}
		}
		
		ArrayList<Bid> objects = new ArrayList<Bid>(),result = new ArrayList<Bid>();
		objects.add(this.bestBid);
		for(Action action :this.latestBids.values()) 
			if(Action.getBidFromAction(action)!=null && getUtility(Action.getBidFromAction(action))>=Math.pow(0.6,timeDone))
				objects.add(Action.getBidFromAction(action));
		System.out.println("Objets " + objects.size());
		System.out.println("TimeUtil " + Math.pow(0.8,timeDone));
		for(int i=0;i<objects.size()-1;i++)
			for(int j=i+1;j<objects.size();j++) {
				result.addAll(crossover(objects.get(i), objects.get(j)));
			}
		
		Collections.sort(result,this.comparator);
		
		System.out.println("Result 1 " + result.size());
		if (initialCrossOverResult != null) {
			result.addAll(initialCrossOverResult);
		}
		
		Collections.sort(result,this.comparator);
		
		System.out.println("Result 2 " + result.size());
		
		return getRandomBestBid(result.subList(0, result.size() / 3));
	}
	
/*	private Bid getGeneticBestBid() {
		ArrayList<Issue> a= this.domain.getIssues();
		HashMap<Integer, Value> randomHIV = new HashMap<Integer, Value>();
		Random random = new Random();
		
		for(Issue issue :a) {
			int ran = Math.ra
		}
		return this.bestBid;
	}*/
	@Override
	public Action chooseAction(List<Class<? extends Action>> validActions) {
		double timeDone= (new Date().getTime() - startTime.getTime())*0.001;
		if (this.acceptable() || ((this.worstUtil>this.reservationValue) && (this.timeLimit - timeDone <= this.timeThreshold))) {
			return new Accept();
		}
		/*
		//Bid result = this.getRandomBestBid();
		//Bid result = this.getGeneticBestBid();
		HashMap<Integer, Value> randomHIV = new HashMap<Integer, Value>();
		for(Issue issue : this.domain.getIssues()) {
			try {
				randomHIV.put(new Integer(issue.getNumber()), getRandomValue(issue));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		
		Bid result;
		try {
			result = new Bid(this.domain,randomHIV);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			result = this.bestBid;
			e.printStackTrace();
		}*/
		this.rounds++;
		return new Offer(getGeneticBestBid(this.rounds));
	}

	public AgentID getPartyId() {
		return this.partyId;
	}

}
