package ru.croccode.hypernull.reader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class PropertiesReader {
    public static Map<String, String> readProperties(String way) throws FileNotFoundException {
        Scanner inFile = new Scanner(new FileReader(way));

        Map<String, String> result = new HashMap<>();

        while (inFile.hasNextLine()) {
            String line = inFile.nextLine();
            String[] tmp = line.split("=");

            if (tmp.length == 2) {
                result.put(tmp[0], tmp[1]);
            }
        }

        return result;
    }
}
