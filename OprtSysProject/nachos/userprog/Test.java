package nachos.userprog;

public class Test {
	public static void selfTest_coff() {
		System.out.println("----------UserTask.start---------------------");
		UserProcess up = UserProcess.newUserProcess();
		up.execute("cp.coff", new String[] { "2", "self.txt", "self_test2.txt" });
		up.execute("cat.coff", new String[] { "2", "self_test.txt" });
		System.out.println("----------UserTask.finish---------------------");

	}
}
