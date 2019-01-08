package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Stats extends ScoreBoardEventProvider {
    public ScoreBoard getScoreBoard();
    public void reset();

    public void ensureAtLeastNPeriods(int n);
    public void truncateAfterNPeriods(int n);

    public Period getPeriod(int p);

    public enum Child implements AddRemoveProperty {
        PERIOD;
    }
}
