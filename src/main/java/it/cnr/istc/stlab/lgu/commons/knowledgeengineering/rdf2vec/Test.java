package it.cnr.istc.stlab.lgu.commons.knowledgeengineering.rdf2vec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Test {
	public static void main(String[] args) throws IOException, InterruptedException {
		Process p = Runtime.getRuntime().exec("pwd");

		Runnable rerr = () -> {
			BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String buff = null;
			try {
				Thread.sleep(1000);
				while ((buff = err.readLine()) != null) {
					System.err.println(buff);
					Thread.sleep(100);
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		};

		Runnable rin = () -> {
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String buff = null;
			try {
				while ((buff = in.readLine()) != null) {
					System.out.println(buff);
					Thread.sleep(100);
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}

		};

		ExecutorService executorService = Executors.newFixedThreadPool(2);
		executorService.execute(rin);
		executorService.execute(rerr);
		System.out.println("ehy");
		p.waitFor();
		System.out.println("ehy");
		executorService.shutdown();
	}
}
