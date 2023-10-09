package com.github.vcoppe;

import gurobi.GRBException;

import java.io.File;
import java.util.Locale;
import java.util.Scanner;

public class Main {

    private static int nPeriods, nItems;
    private static int[][] setupCost, demand;
    private static int[] stockingCost;

    private static void read(String path) {
        try {
            Scanner scan = new Scanner(new File(path));

            nPeriods = scan.nextInt();
            nItems = scan.nextInt();

            setupCost = new int[nItems][nPeriods];
            stockingCost = new int[nItems];
            demand = new int[nItems][nPeriods];

            scan.nextInt(); // nOrders

            for (int i = 0; i < nItems; i++) for (int j = 0; j < nItems; j++) {
                setupCost[i][j] = scan.nextInt();
            }

            for (int i = 0; i < nItems; i++) {
                stockingCost[i] = scan.nextInt();
            }

            for (int i = 0; i < nItems; i++) for (int j = 0; j < nPeriods; j++) {
                demand[i][j] = scan.nextInt();
            }

            scan.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to read input file");
            System.exit(0);
        }
    }

    public static void main(String[] args) throws GRBException {
        if (args.length < 1) {
            System.out.println("Arguments needed :\n\tfilename\n\t[timeLimit]\n\t[threads]");
            return;
        }

        read(args[0]);

        int timeLimit = Integer.MAX_VALUE, threads = 0;
        if (args.length >= 2) timeLimit = Integer.parseInt(args[1]);
        if (args.length == 3) threads = Integer.parseInt(args[2]);

        Model mip = new Model(nPeriods, nItems, setupCost, stockingCost, demand);

        mip.solve(timeLimit, threads);

        Locale.setDefault(Locale.US);

        int objVal = (int) Math.round(mip.objVal());
        int bestBound = (int) Math.round(mip.lowerBound());
        System.out.printf("solver     : %s\n", "mip");
        System.out.printf("threads    : %d\n", threads);
        System.out.println("width      : 0");
        System.out.println("caching    : false");
        System.out.println("dominance  : false");
        System.out.println("cmpr. bound: false");
        System.out.println("cmpr. heu. : false");
        System.out.println("cmpr. width: 0");
        System.out.println("nb clusters: 0");
        System.out.printf("is exact   : %b\n", mip.hasProved());
        System.out.printf("duration   : %f\n", mip.runTime());
        System.out.printf("best value : %d\n", objVal);
        System.out.printf("best bound : %d\n", bestBound);
        System.out.println("expl. b&b  : 0");
        System.out.println("expl. dd   : 0");
        System.out.println("peak mem.  : 0");

        mip.dispose();
    }

}
