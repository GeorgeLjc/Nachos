package nachos.threads;

import java.util.LinkedList;
import java.util.Scanner;

import nachos.ag.BoatGrader;
import nachos.machine.Lib;
import nachos.machine.Machine;

public class Boat {
	protected static class person implements  Runnable {//represent people 
		int type=-1;
		int location=0;//0: Oahu , 1:Molokai
		/*
		 * @type 0:child 1:adult
		 */
		person(int type){
			this.type=type;
			location=0;
		}
		
		public void run() {
			if(type==0){
				//child go
				childGo(this);
			}else{
				//adult go
				adultGo(this);
			}
		}
	}
	static BoatGrader bg;
	static int adultOnOahu,childOnOahu,adultOnMolo,childOnMolo;
	static int boatLoca,boatPeoNum;
	static Lock boatLock,adultBanLock,adultAllowOnLock,waitForRowlock;
	static Condition boatlockCond,adultBanCond,adultAllowOnCond,waitforRowCond;
	static Lock waitRiderLock,finishLock;
	static Condition waitRiderCond,finishCond;
	static LinkedList<person> boat =new LinkedList<person>();
	static boolean gameOver=false;
	static int waitToBackTheBoat=0;

	/*
	 * adult part
	 */
	static void adultGo(person adult){
		Lib.assertTrue(adult.location==0);
		adultOnBoat(adult);//大人登船
		new BoatGrader().AdultRowToMolokai();
		adultRowToMolo(adult);//大人将船划到M岛
	}
	static void adultOnBoat(person adult){
		boatLock.acquire();
		while(childOnOahu!=1||boatLoca!=0||boat.size()!=0){
			adultAllowOnLock.acquire();
			System.out.println(KThread.currentThread().getName()+" can not board the boat ");
			boatLock.release();
			/*
			 * 1. the number of children on Oahu not equals to 1 
			 * 2. the boat located in Molokai
			 * 3. had child went on boarded the boat
			 * if those condition appeared , adults can not go on board
			 */
			adultAllowOnCond.sleep();//大人不能登船
			adultAllowOnLock.release();
			boatLock.acquire();
			
		}
		System.out.println(KThread.currentThread().getName()+" successfully board the boat");
		boat.add(adult);
		boatLock.release();
	}
	static void adultRowToMolo(person adult){
		adultOnOahu--;adultOnMolo++;
		boatLoca=1;
		getOff(adult);
		adult.location=1;
	}
	/*
	 * child part
	 */
	static boolean firstChildOnBoat(person child){
		boatLock.acquire();
		if(boat.isEmpty()==false)
		//System.out.println(boat.getFirst().type);
		if(boat.isEmpty()==false)
			while(childOnOahu==1||boatLoca!=0||boat.size()>=2){
				System.out.println(KThread.currentThread().getName()+" can not board the boat");
				adultBanLock.acquire();
				adultBanCond.sleep();
				adultBanLock.release();
			}
		boat.add(child);
		if(boat.size()==2){
			
			System.out.println(KThread.currentThread().getName()+" board the boat (#Ride)");
			boatLock.release();
			
			waitRiderLock.acquire();
			waitRiderCond.wake();//唤醒划船的孩子
			waitRiderLock.release();
			
			waitForRowlock.acquire();
			waitforRowCond.sleep();//等待划船的孩子将船划到M岛
			waitForRowlock.release();
			return false;
		}else{
			boatLock.release();
			System.out.println(KThread.currentThread().getName()+" board the boat (#Row)");
			
			waitRiderLock.acquire();
			waitRiderCond.sleep();//等待坐船的孩子登船
			waitRiderLock.release();
			
			return true;
		}
	}
	static void childRow(person child,int destination){
		boatLoca=destination;
		child.location=destination;
		if(destination==1) {
			childOnOahu--;childOnMolo++;
		}else{
			childOnOahu++;childOnMolo--;
		}
		getOff(child);
		boatLock.acquire();
		waitForRowlock.acquire();
		if(destination==1) waitforRowCond.wake();//船已经到了M岛，唤醒坐船的孩子下船
		waitForRowlock.release();
		boatLock.release();
	}
	static void childRide(person child,int destination){
		child.location=destination;
		if(destination==1) {
			childOnOahu--;childOnMolo++;
		}else{
			childOnOahu++;childOnMolo--;
		}
		getOff(child);
	}
	static void childGo(person child){
		//Lib.assertTrue(child.location==0);
		//int i=0;
		while(!gameOver){
			if(child.location==0&&boatLoca==0){
				if(firstChildOnBoat(child)){
					new BoatGrader().ChildRowToMolokai();
					childRow(child,1);
					//BoatArriveMolo(child);
				}else{
					new BoatGrader().ChildRideToMolokai();
					childRide(child,1);
					//BoatArriveMolo(child);
				}
			}else if(childOnOahu+adultOnOahu>0&&boatLoca==1&&child.location==1){

/**/		System.out.println("now boat :"+boatLoca+" by thread : "+KThread.currentThread().getName());
//			System.out.println(KThread.currentThread().getName());
			BoatArriveMolo(child);
			new BoatGrader().ChildRowToOahu();
			childRow(child, 0);//back to Oahu
			
			}
			//if(i++==0)
			//System.out.println("test");
			KThread.yield();
		}
	} 	
	/*
	 * other methods
	 */
	static void BoatArriveMolo(person child){
		//if(child.location==0) return ;
		boatLock.acquire();
		System.out.println(KThread.currentThread().getName()+" board the boat");
		boat.add(child);
		boatLock.release();
	}
	static void getOff(person person){
		boatLock.acquire();
		System.out.println("Boat arrive : "+(boatLoca==0?"Oahu":"Molokai"));
		boat.remove(person);
		if(boat.isEmpty()==false){ boatLock.release();return ;}
		if(boatLoca==0){
/**/	//	System.out.println("Oahu child "+childOnOahu);
			if(childOnOahu!=1)	{
				adultBanLock.acquire();
				adultBanCond.wake();
				adultBanLock.release();
			}
			else{
				adultAllowOnLock.acquire();
				adultAllowOnCond.wake();//child on oahu equals  1 
				adultAllowOnLock.release();
			}
		}else {
			//System.out.println("adultO : "+adultOnOahu+" , childO : "+childOnOahu);
			if(adultOnOahu+childOnOahu==0){
				gameOver=true;
				finishLock.acquire();
				finishCond.wake();
				finishLock.release();
			}
		}
		boatLock.release();
	}
	/*
	 * self test
	 */
	public static void selfTest() {
		BoatGrader b = new BoatGrader();
		System.out.println("\n ***Testing Boats with only 2 children***");
		System.out.println("\n input the number of adult : ");
		
		int adultNum=4;
		boolean status = Machine.interrupt().disable();
		begin(adultNum, 2, b);	
		Machine.interrupt().restore(status);
		// System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		// begin(1, 2, b);  

		// System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		// begin(3, 3, b);
	}
	

	public static void begin(int adults, int children, BoatGrader b) {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
//		bg = b;

		// Instantiate global variables here

		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.

//		Runnable r = new Runnable() {
//			public void run() {
//				SampleItinerary();
//			}
//		};
//		KThread t = new KThread(r);
//		t.setName("Sample Boat Thread");
//		t.fork();
		adultOnMolo=childOnMolo=0;
		adultOnOahu=adults;
		childOnOahu=2;
		boatLoca=0;
		boatPeoNum=0;
		
		boatLock=new Lock();
		adultBanLock=new Lock();
		adultAllowOnLock=new Lock();
		waitForRowlock=new Lock();
		waitRiderLock=new Lock();
		finishLock=new Lock();
		
		adultBanCond=new Condition(adultBanLock);//孩子可以登船
		adultAllowOnCond=new Condition(adultAllowOnLock);//成人可以登船
		waitforRowCond=new Condition(waitForRowlock);//坐船的孩子等待划船的孩子划船
		waitRiderCond=new Condition(waitRiderLock);//划船的孩子等待坐船的孩子登船
		finishCond=new Condition(finishLock);//游戏是否结束
		
		
		for(int i=1;i<=adults;++i){
			KThread adult = new KThread(new person(1));
			adult.setName("Adult-"+i);
			adult.fork();
		}
		for(int i=1;i<=children;++i){
			KThread child=new KThread(new person(0));
			child.setName("Child-"+i);
			child.fork();
		}
		finishLock.acquire();
		while(!gameOver){
			finishCond.sleep();
			if(childOnMolo==2&&adultOnMolo==adults){
				gameOver=true;
			}else{
				System.out.println("========*****======== Game continue ========*****========");
			}
		}
		finishLock.release();
		System.out.println("========*****======== Game finish　========＊＊＊＊＊========");
		
	}

	static void AdultItinerary() {
		bg.initializeAdult(); // Required for autograder interface. Must be the
								// first thing called.
		// DO NOT PUT ANYTHING ABOVE THIS LINE.

		/*
		 * This is where you should put your solutions. Make calls to the
		 * BoatGrader to show that it is synchronized. For example:
		 * bg.AdultRowToMolokai(); indicates that an adult has rowed the boat
		 * across to Molokai
		 */
	}

	static void ChildItinerary() {
		bg.initializeChild(); // Required for autograder interface. Must be the
								// first thing called.
		// DO NOT PUT ANYTHING ABOVE THIS LINE.
	}

	static void SampleItinerary() {
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out
				.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}
	

}
