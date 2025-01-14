/*
 * Copyright (c) 2023, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.runtime.profiler.runtime;

import io.ballerina.identifier.Utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is used as the main profiler class for the Ballerina profiler.
 *
 * @since 2201.8.0
 */
public class ProfileAnalyzer {

    private final HashMap<String, Data> profiles = new HashMap<>();
    private final ArrayList<Data> profilesStack = new ArrayList<>();
    private final ArrayList<String> blockedMethods = new ArrayList<>();
    private static final List<String> skippedList = new ArrayList<>();
    private static final Set<String> skippedClasses = new HashSet<>(skippedList);

    private static class ProfilerHolder {
        private static final ProfileAnalyzer PROFILER_INSTANCE = new ProfileAnalyzer();
    }

    public static ProfileAnalyzer getInstance() {
        return ProfilerHolder.PROFILER_INSTANCE;
    }

    private ProfileAnalyzer() {
        addProfilerShutDownHook();
        try {
            String content = Files.readString(Paths.get("usedPathsList.txt"));
            List<String> skippedListRead = new ArrayList<>(Arrays.asList(content.split(", ")));
            skippedList.addAll(skippedListRead);
            skippedClasses.addAll(skippedList);
        } catch (IOException ignored) {
            throw new ProfilerRuntimeException("Error occurred while reading the usedPathsList.txt file");
        }
    }

    private String getMethodName() {
        final List<StackWalker.StackFrame> stack = StackWalker.getInstance().walk(s -> s.collect(Collectors.toList()));
        return stack.get(2).getMethodName() + "()";
    }

    private void removeDuplicates(List<String> list) {
        List<String> newList = new ArrayList<>();
        for (String element : list) {
            if (!newList.contains(element)) {
                newList.add(element);
            }
        }
        list.clear();
        list.addAll(newList);
    }

    public void start(int id) {
        String name = getStackTrace();
        if (!blockedMethods.contains(getMethodName() + id)) {
            Data p = this.profiles.computeIfAbsent(name, key -> {
                Data newData = new Data(key);
                profilesStack.add(newData);
                return newData;
            });
            p.start();
            removeDuplicates(blockedMethods);
        }
        blockedMethods.remove(getMethodName() + id);
    }

    public void start() {
        String name = getStackTrace();
        Data p = this.profiles.get(name);
        if (p == null) {
            p = new Data(name);
            this.profiles.put(name, p);
            this.profilesStack.add(p);
        }
        p.start();
        removeDuplicates(blockedMethods);
    }

    public void stop(String strandState, int id) {
        String name = getStackTrace();
        Data p = this.profiles.get(name);
        if (strandState.equals("RUNNABLE")) {
            if (p != null) {
                p.stop();
            }
        } else {
            blockedMethods.add(getMethodName() + id);
        }
    }

    public void stop() {
        String name = getStackTrace();
        Data p = this.profiles.get(name);
        if (p != null) {
            p.stop();
        }
    }

    public final String getProfileStackString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < this.profilesStack.size(); i++) {
            sb.append(this.profilesStack.get(i)).append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private void printProfilerOutput(String dataStream) {
        try (Writer myWriter = new FileWriter("CpuPre.json", StandardCharsets.UTF_8)) {
            myWriter.write(dataStream);
        } catch (IOException e) {
            throw new ProfilerRuntimeException("Error occurred while writing to the CpuPre.json file");
        }
    }

    private void addProfilerShutDownHook() {
        // add a shutdown hook to stop the profiler and parse the output when the program is closed
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ProfileAnalyzer profiler = ProfileAnalyzer.getInstance();
            profiler.printProfilerOutput(profiler.getProfileStackString());
        }));
    }

    // This method returns a string representation of the current call stack in the form of a list of strings
    private String getStackTrace() {
        ArrayList<String> result = new ArrayList<>();

        final List<StackWalker.StackFrame> stack = StackWalker.getInstance().walk(s -> s.collect(Collectors.toList()));
        //Removes the first 2 stack frames (index 0 and 1) and reverses the order of the remaining stack frames
        stack.subList(0, 2).clear();
        Collections.reverse(stack); //Reverse the collection

        for (StackWalker.StackFrame frame : stack) {
            if (skippedClasses.contains(frame.getClassName())) {
                String frameString = frame.toString();

                if (frameString.contains("&")) {
                    frameString = "\"" + frameString + "\"";
                } else {
                    frameString = "\"" + frameString.replaceAll("\\(.*\\)", "") + "()" + "\"";
                    int lastDotIndex = frameString.lastIndexOf('.');
                    frameString = frameString.substring(0, lastDotIndex).replace('.', '/') +
                            frameString.substring(lastDotIndex);
                }
                result.add(Utils.decodeIdentifier(frameString));
            }
        }

        return result.toString();
    }
}
