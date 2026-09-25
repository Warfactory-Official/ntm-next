// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.module.mses;

public interface CompiledMses {
    int OK = 0;
    int UNRECOGNIZED_COMMAND = 1;
    int PARAMETER_ERROR = 2;
    int END_TICK = 3;
    int SHUTDOWN = 4;
    int SKIP = 5;
    int UNDEFINED = 6;
    int STACK_EXCEEDED = 7;

    void run(MsesState state, Host host);

    interface Host {

        void msesAfterInstruction(int sourceIndex, int result);

        void msesEndOfProgram(boolean outOfBounds);

        void msesEvaluationFailed();
    }
}
