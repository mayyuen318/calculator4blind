package may.speechcalculator;

import android.text.TextUtils;
import android.util.Log;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FormulaFilter {
    private ArrayList<String> stopList = new ArrayList<>();
    private HashMap<String, String[]> matchList = new HashMap<>();
    private ArrayList<String> ignoreList = new ArrayList<>();

    private ArrayList<String[]>[] orderedMatchList = new ArrayList[3]; // Longest is 3

    public FormulaFilter() {
        stopList.add("equal");
        stopList.add("eagle");
        stopList.add("ecos");

        matchList.put("+", new String[]{"plus", "and", "press"});
        matchList.put("-", new String[]{"minus"});
        matchList.put("*", new String[]{"time"});
        matchList.put("/", new String[]{"divide"});
        matchList.put("^", new String[]{"power", "pala"});
        matchList.put("(", new String[]{"open bracket"});
        matchList.put(")", new String[]{"close bracket", "cross bracket"});

        matchList.put("sqrt($1)", new String[]{"square root ([\\d.]+)"});
        matchList.put("sine($1)", new String[]{"psy ([\\d.]+)", "sigh ([\\d.]+)", "sci ([\\d.]+)", "size ([\\d.]+)"});
        matchList.put("tangent($1)", new String[]{"tangent ([\\d.]+)", "panjin ([\\d.]+)"});
        matchList.put("cosine($1)", new String[]{"cosine ([\\d.]+)", "cossy ([\\d.]+)", "cosin ([\\d.]+)", "cosign ([\\d.]+)", "kosai ([\\d.]+)"});

        matchList.put("^2", new String[]{"square"});
        matchList.put("^3", new String[]{"cube"});

        matchList.put("1", new String[]{"one"});
        matchList.put("2", new String[]{"two", "total", "tool", "you"});
        matchList.put("3", new String[]{"three", "frame", "free"});
        matchList.put("4", new String[]{"four", "full", "thought", "for"});
        matchList.put("5", new String[]{"five"});
        matchList.put("6", new String[]{"six"});
        matchList.put("7", new String[]{"seven"});
        matchList.put("8", new String[]{"eight"});
        matchList.put("9", new String[]{"nine"});
        matchList.put("10", new String[]{"ten"});
        matchList.put("11", new String[]{"eleven"});
        matchList.put("12", new String[]{"twelve", "shelf", "chauf", "chove", "show"});
        matchList.put("13", new String[]{"thirteen"});
        matchList.put("14", new String[]{"fourteen"});
        matchList.put("15", new String[]{"fifteen"});
        matchList.put("16", new String[]{"sixteen"});
        matchList.put("17", new String[]{"seventeen"});
        matchList.put("18", new String[]{"eighteen"});
        matchList.put("19", new String[]{"nineteen"});

        // TODO: Write a program to convert string to numbers
        matchList.put("20", new String[]{"twenty"});
        matchList.put("21", new String[]{"twenty one"});
        matchList.put("22", new String[]{"twenty two"});

        ignoreList.add("to");
        ignoreList.add("of");
        ignoreList.add("the");
        ignoreList.add("degree");
        ignoreList.add("degrees");

        try {
            this.initOrderedMatchList();
        } catch(Exception e) { Log.d("Formula", "Increase the orderedMatchList size, stupid!"); }
    }

    private void initOrderedMatchList() throws Exception{
        for(int i=0; i < orderedMatchList.length; i++)
            orderedMatchList[i] = new ArrayList<>();

        for(Map.Entry<String, String[]> pairs : matchList.entrySet()) {
            for (String value : pairs.getValue()) {
                int tokenLength = value.length() - value.replace(" ", "").length();
                if(tokenLength > orderedMatchList.length) { throw new Exception("token length too long"); }

                orderedMatchList[tokenLength].add(new String[] {value, pairs.getKey()});
            }
        }
    }

    public String speechResultToFormula(String text) {
        return translateTokens(tokenizeString(text));
    }

    private String[] tokenizeString(String text) {
        String[] tokens = text.split("\\s");

        for(int i=0; i < tokens.length; i++) {
            tokens[i] = tokens[i].toLowerCase().trim();
        }

        return tokens;
    }

    private String translateTokens(String[] tokens) {
        ArrayList<String> translatedTokens = new ArrayList<>();
        for(String token: tokens) {
            boolean inStopList = false;
            for(String item : stopList) {
                if(token.contains(item)) inStopList = true;
            }
            if(inStopList) break;

            boolean inIgnoreList = false;
            for(String item : ignoreList) {
                if(token.equals(item)) inIgnoreList = true;
            }
            if(!inIgnoreList) translatedTokens.add(token);
        }

        return matchAndReplace(TextUtils.join(" ", translatedTokens)).replace(" ", "");
    }

    public double evaluateFormula(String expression) {
        ArrayList<Function> functions = new ArrayList<>();

        functions.add(new Function("cosine", 1) {
            @Override
            public double apply(double... doubles) {
                return Math.cos(Math.toRadians(doubles[0]));
            }
        });

        functions.add(new Function("sine",1){
            @Override
            public double apply(double... doubles) {
                return Math.sin(Math.toRadians(doubles[0]));
            }
        });

        functions.add(new Function("tangent",1) {
            @Override
            public double apply(double... doubles) {
                return Math.tan(Math.toRadians(doubles[0]));
            }
        });
        Expression e = new ExpressionBuilder(expression).functions(functions).build();
        return e.evaluate();
    }

    private String matchAndReplace(String token) {
        for(int i=orderedMatchList.length - 1; i >= 0; i--) {
            for(String[] pair: orderedMatchList[i]) {
                token = token.replaceAll("\\w*" + pair[0] + "\\w*", " " + pair[1] + " ");
            }
        }
        return token;
    }
}
