/*
 * Copyright 2012 Luca Zanconato
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.nharyes.drivecopy.biz.bo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DirectoryBO implements BusinessObject {

    private File file;

    private File destinationDirectory;

    private int level;

    private List<File> notCompressed = new ArrayList<>();

    public File getFile() {

        return file;
    }

    public void setFile(File file) {

        this.file = file;
    }

    public int getLevel() {

        return level;
    }

    public void setLevel(int level) {

        this.level = level;
    }

    public File getDestinationDirectory() {

        return destinationDirectory;
    }

    public void setDestinationDirectory(File destinationDirectory) {

        this.destinationDirectory = destinationDirectory;
    }

    public List<File> getNotCompressed() {

        return notCompressed;
    }

    public void setNotCompressed(List<File> notCompressed) {

        this.notCompressed = notCompressed;
    }
}
