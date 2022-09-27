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

        String[] split = args[0].split("/");
        String instance = split[split.length - 1];

        Locale.setDefault(Locale.US);

        int objVal = (int) - Math.round(mip.objVal());
        int bestBound = (int) - Math.round(mip.lowerBound());
        System.out.printf("%s | mip | %s | %.2f | 0 | %d | %d | %d | %.4f\n",
                instance,
                mip.hasProved() ? "Proved" : "Timeout",
                mip.runTime(),
                objVal,
                objVal,
                bestBound,
                mip.gap()
        );

        mip.dispose();
    }

}
