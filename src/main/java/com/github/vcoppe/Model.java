package com.github.vcoppe;

import gurobi.*;

public class Model {

    private GRBEnv env;
    private GRBModel model;
    private GRBVar[] y_init, s_init, w_init;
    private GRBVar[][] x, y, s, z, w;
    private GRBVar[][][] X;

    public Model(int nPeriods, int nItems, int[][] setupCost, int[] stockingCost, int[][] demand) throws GRBException {
        env = new GRBEnv("psp.log");
        model = new GRBModel(env);

        int[][] cumul = new int[nItems][nPeriods];
        for (int i = 0; i < nItems; i++) for (int t = 0; t < nPeriods; t++) {
            if (t > 0) cumul[i][t] = cumul[i][t - 1] + demand[i][t];
            else cumul[i][t] = demand[i][t];
        }

        x = new GRBVar[nItems][nPeriods];
        y = new GRBVar[nItems][nPeriods];
        y_init = new GRBVar[nItems];
        s = new GRBVar[nItems][nPeriods];
        s_init = new GRBVar[nItems];
        z = new GRBVar[nItems][nPeriods];
        w = new GRBVar[nItems][nPeriods];
        w_init = new GRBVar[nItems];
        X = new GRBVar[nItems][nItems][nPeriods];

        for (int i = 0; i < nItems; i++) {
            y_init[i] = model.addVar(0, 1, 0, GRB.BINARY, "y_init_" + i);
            s_init[i] = model.addVar(0, 0, 0, GRB.BINARY, "s_init_" + i);
            w_init[i] = model.addVar(0, 0, 0, GRB.BINARY, "w_init_" + i);

            for (int t = 0; t < nPeriods; t++) {
                x[i][t] = model.addVar(0, 1, 0, GRB.BINARY, "x_" + i + "_" + t);
                y[i][t] = model.addVar(0, 1, 0, GRB.BINARY, "y_" + i + "_" + t);
                s[i][t] = model.addVar(0, cumul[i][nPeriods - 1], stockingCost[i], GRB.INTEGER, "s_" + i + "_" + t);
                z[i][t] = model.addVar(0, 1, 0, GRB.BINARY, "z_" + i + "_" + t);
                w[i][t] = model.addVar(0, 1, 0, GRB.BINARY, "w_" + i + "_" + t);

                for (int j = 0; j < nItems; j++) {
                    X[i][j][t] = model.addVar(0, 1, setupCost[i][j], GRB.BINARY, "X_" + i + "_" + j + "_" + t);
                }
            }
        }

        GRBLinExpr expr;
        for (int i = 0; i < nItems; i++) for (int t = 0; t < nPeriods; t++) {
            expr = new GRBLinExpr();
            if (t > 0) expr.addTerm(1, s[i][t - 1]);
            else expr.addTerm(1, s_init[i]);
            expr.addTerm(1, x[i][t]);
            expr.addTerm(-1, s[i][t]);
            model.addConstr(expr, GRB.EQUAL, demand[i][t], "stock_conservation_" + i +"_" + t);

            model.addConstr(x[i][t], GRB.LESS_EQUAL, y[i][t], "can_produce_" + i + "_" + t);

            expr = new GRBLinExpr();
            for (int j = 0; j < nItems; j++) {
                expr.addTerm(1, X[i][j][t]);
            }
            if (t > 0) model.addConstr(expr, GRB.EQUAL, y[i][t - 1], "transition_start_" + i + "_" + t);
            else model.addConstr(expr, GRB.EQUAL, y_init[i], "transition_start_" + i + "_" + t);

            expr = new GRBLinExpr();
            for (int j = 0; j < nItems; j++) {
                expr.addTerm(1, X[j][i][t]);
            }
            model.addConstr(expr, GRB.EQUAL, y[i][t], "transition_end_" + i + "_" + t);

            expr = new GRBLinExpr();
            expr.addTerm(1, y[i][t]);
            expr.addTerm(-1, z[i][t]);
            model.addConstr(expr, GRB.EQUAL, X[i][i][t], "set_z_" + i + "_" + t);

            expr = new GRBLinExpr();
            if (t > 0) {
                expr.addTerm(1, y[i][t - 1]);
                expr.addTerm(-1, w[i][t - 1]);
            } else {
                expr.addTerm(1, y_init[i]);
                expr.addTerm(-1, w_init[i]);
            }
            model.addConstr(expr, GRB.EQUAL, X[i][i][t], "set_w_" + i + "_" + t);
        }

        for (int t = 0; t < nPeriods; t++) {
            expr = new GRBLinExpr();
            for (int i = 0; i < nItems; i++) {
                expr.addTerm(1, y[i][t]);
            }
            model.addConstr(expr, GRB.EQUAL, 1, "single_production_" + t);
        }

        expr = new GRBLinExpr();
        for (int i = 0; i < nItems; i++) {
            expr.addTerm(1, y_init[i]);
        }
        model.addConstr(expr, GRB.EQUAL, 1, "single_initial_production");

        for (int i = 0; i < nItems; i++) for (int t = 0; t < nPeriods; t++) {
            for (int l = t + 1; l < nPeriods; l++) if (demand[i][l] == 1) {
                expr = new GRBLinExpr();
                if (t > 0) expr.addTerm(1, s[i][t - 1]);
                else expr.addTerm(1, s_init[i]);

                int p = cumul[i][l] - (t > 0 ? cumul[i][t - 1] : 0);
                for (int u = t; u < t + p; u++) {
                    expr.addTerm(1, y[i][u]);
                }

                for (int u = t + 1; u < t + p; u++) {
                    expr.addConstant(cumul[i][l] - cumul[i][u - 1] - (t + p - u));
                }

                for (int u = t + p; u <= l; u++) {
                    expr.addTerm(cumul[i][l] - cumul[i][u - 1], z[i][u]);
                }

                model.addConstr(expr, GRB.GREATER_EQUAL, p, "no_overproduction_" + i + "_" + t);
            }

        }
    }

    public void solve(double timeLimit, int threads) throws GRBException {
        model.set(GRB.IntParam.OutputFlag, 0);
        model.set(GRB.DoubleParam.FeasibilityTol, 1e-9);
        model.set(GRB.DoubleParam.IntFeasTol, 1e-9);
        model.set(GRB.DoubleParam.OptimalityTol, 1e-9);
        model.set(GRB.DoubleParam.TimeLimit, timeLimit);
        if (threads > 0) model.set(GRB.IntParam.Threads, threads);
        model.optimize();
    }

    public double gap() throws GRBException {
        return model.get(GRB.DoubleAttr.MIPGap);
    }

    public double runTime() throws GRBException {
        return model.get(GRB.DoubleAttr.Runtime);
    }

    public double objVal() throws GRBException {
        return model.get(GRB.DoubleAttr.ObjVal);
    }

    public double lowerBound() throws GRBException {
        return model.get(GRB.DoubleAttr.ObjBound);
    }

    public boolean hasProved() throws GRBException {
        return model.get(GRB.IntAttr.Status) == GRB.OPTIMAL;
    }

    public void dispose() throws GRBException {
        model.dispose();
        env.dispose();
    }
}
