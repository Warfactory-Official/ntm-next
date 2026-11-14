/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.handler.radio;

public sealed interface RadioSignal {
    String text();

    record Text(String text) implements RadioSignal {}

    record Char(char value) implements RadioSignal {
        @Override
        public String text() {
            return String.valueOf(value);
        }
    }

    record Int32(int value) implements RadioSignal {
        @Override
        public String text() {
            return Integer.toString(value);
        }
    }

    record Int64(long value) implements RadioSignal {
        @Override
        public String text() {
            return Long.toString(value);
        }
    }

    record Decimal(double value) implements RadioSignal {
        @Override
        public String text() {
            return Double.toString(value);
        }
    }
}
