/*
 * Copyright 2012-2016 Luca Zanconato
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

package net.nharyes.drivecopy.log;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

public class SystemOutHandler extends ConsoleHandler {

    public SystemOutHandler() {

        super();

        // output to System.out
        setOutputStream(System.out);

        // level
        setLevel(Level.FINER);

        // tiny formatter
        setFormatter(new TinyFormatter());
    }
}
