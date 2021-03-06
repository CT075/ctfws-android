package com.acmetensortoys.ctfwstimer;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.acmetensortoys.ctfwstimer.lib.CtFwSGameState;

import java.util.List;

import static android.view.View.INVISIBLE;

// TODO nwf is bad at UI design; someone who isn't him should improve this
class CtFwSDisplayLocal implements CtFwSGameState.Observer {
    final private Activity mAct;
    String gameStateLabelText;

    CtFwSDisplayLocal(Activity a) {
        mAct = a;
        gameStateLabelText = mAct.getResources().getString(R.string.header_gamestate0);
    }

    @Override
    public void onCtFwSConfigure(final CtFwSGameState gs) {
        int gameix = gs.getGameIx();
        gameStateLabelText =
                (gs.isConfigured() && gameix != 0)
                        ?
                        String.format(
                                mAct.getResources()
                                        .getString(R.string.header_gamestateN),
                                gameix)
                        : mAct.getResources().getString(R.string.header_gamestate0);


        final TextView gstv = (TextView) mAct.findViewById(R.id.header_gamestate);
        gstv.post(new Runnable() {
            @Override
            public void run() {
                gstv.setText(gameStateLabelText);
            }
        });
    }

    @Override
    public void onCtFwSNow(final CtFwSGameState gs, final CtFwSGameState.Now now) {
        // time base correction factor ("when we booted"-ish)
        final long tbcf = System.currentTimeMillis() - SystemClock.elapsedRealtime();

        Log.d("CtFwS", "Display game state; nowMS=" + now.wallMS + " r=" + now.round + " rs=" + now.roundStart + " re=" + now.roundEnd);

        if (now.rationale != null) {
            Log.d("CtFwS", "Rationale: " + now.rationale + " stop=" + now.stop);
            // TODO: display rationale somewhere, probably by hiding the game state!
            doReset();
            return;
        }
        // Otherwise, it's game on!

        // Upper line text
        {
            final TextView tv_jb = (TextView) (mAct.findViewById(R.id.tv_jailbreak));
            tv_jb.post(new Runnable() {
                @Override
                public void run() {
                    if (now.round == 0) {
                        tv_jb.setText(R.string.ctfws_gamestart);
                    } else if (now.round == gs.getRounds()) {
                        tv_jb.setText(R.string.ctfws_gameend);
                    } else {
                        tv_jb.setText(
                                String.format(mAct.getResources().getString(R.string.ctfws_jailbreak),
                                        now.round, gs.getRounds() - 1));
                    }
                }
            });
        }

        // Upper progress bar and chronometer
        {
            final ProgressBar pb_jb = (ProgressBar) (mAct.findViewById(R.id.pb_jailbreak));
            pb_jb.post(new Runnable() {
                @Override
                public void run() {
                    pb_jb.setIndeterminate(false);
                    pb_jb.setMax((int) (now.roundEnd - now.roundStart));
                    pb_jb.setProgress(0);
                }
            });

            final Chronometer ch_jb = (Chronometer) (mAct.findViewById(R.id.ch_jailbreak));
            ch_jb.post(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        ch_jb.setBase((now.roundEnd + 1) * 1000 - tbcf);
                        ch_jb.setCountDown(true);
                    } else {
                        ch_jb.setBase(now.roundStart * 1000 - tbcf);
                        ch_jb.setBackgroundColor(Color.BLACK);
                        ch_jb.setTextColor(Color.WHITE);
                    }
                    ch_jb.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                        @Override
                        public void onChronometerTick(Chronometer c) {
                            pb_jb.setProgress((int) (now.roundEnd - System.currentTimeMillis() / 1000));
                        }
                    });
                    ch_jb.start();
                }
            });
        }

        // Lower progress bar and chronometer
        if (now.round > 0) {
            final ProgressBar pb_gp = (ProgressBar) (mAct.findViewById(R.id.pb_gameProgress));
            pb_gp.post(new Runnable() {
                @Override
                public void run() {
                    pb_gp.setIndeterminate(false);
                    pb_gp.setMax(gs.getComputedGameDuration());
                    pb_gp.setProgress(0);
                }
            });

            final Chronometer ch_gp = (Chronometer) (mAct.findViewById(R.id.ch_gameProgress));
            ch_gp.post(new Runnable() {
                @Override
                public void run() {
                    ch_gp.setBase(gs.getFirstRoundStartT() * 1000 - tbcf);
                    ch_gp.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                        @Override
                        public void onChronometerTick(Chronometer c) {
                            pb_gp.setProgress((int) (System.currentTimeMillis() / 1000
                                    - gs.getFirstRoundStartT()));
                        }
                    });
                    ch_gp.setVisibility(View.VISIBLE);
                    ch_gp.start();
                }
            });
        } else {
            final ProgressBar pb_gp = (ProgressBar) (mAct.findViewById(R.id.pb_gameProgress));
            pb_gp.post(new Runnable() {
                @Override
                public void run() {
                    pb_gp.setIndeterminate(true);
                }
            });

            final Chronometer ch_gp = (Chronometer) (mAct.findViewById(R.id.ch_gameProgress));
            ch_gp.post(new Runnable() {
                @Override
                public void run() {
                    ch_gp.setOnChronometerTickListener(null);
                    ch_gp.stop();
                    ch_gp.setVisibility(INVISIBLE);
                }
            });
        }
        {
            final TextView tv_flags = (TextView) (mAct.findViewById(R.id.tv_flags_label));
            tv_flags.post(new Runnable() {
                @Override
                public void run() {
                    tv_flags.setText(mAct.getResources()
                            .getQuantityString(R.plurals.ctfws_flags,gs.flagsTotal,gs.flagsTotal));
                }
            });
        }
    }

    private void doReset() {
        Log.d("CtFwS", "Display Reset");

        {
            final Chronometer ch = (Chronometer) (mAct.findViewById(R.id.ch_jailbreak));
            ch.post(new Runnable() {
                @Override
                public void run() {
                    ch.setOnChronometerTickListener(null);
                    ch.setBase(SystemClock.elapsedRealtime());
                    ch.stop();
                }
            });
        }
        {
            final Chronometer ch = (Chronometer) (mAct.findViewById(R.id.ch_gameProgress));
            ch.post(new Runnable() {
                @Override
                public void run() {
                    ch.setOnChronometerTickListener(null);
                    ch.stop();
                    ch.setVisibility(View.INVISIBLE);
                }
            });
        }
        {
            final ProgressBar pb = (ProgressBar) (mAct.findViewById(R.id.pb_jailbreak));
            pb.post(new Runnable() {
                @Override
                public void run() {
                    pb.setIndeterminate(true);
                }
            });
        }
        {
            final ProgressBar pb = (ProgressBar) (mAct.findViewById(R.id.pb_gameProgress));
            pb.post(new Runnable() {
                @Override
                public void run() {
                    pb.setIndeterminate(true);
                }
            });
        }
    }

    @Override
    public void onCtFwSFlags(CtFwSGameState gs) {
        // TODO: This stinks

        final StringBuffer sb = new StringBuffer();
        if (gs.isConfigured()) {
            if (gs.flagsVisible) {
                sb.append("r=");
                sb.append(gs.flagsRed);
                sb.append(" y=");
                sb.append(gs.flagsYel);
            } else {
                sb.append("r=? y=?");
            }
        }

        final TextView msgs = (TextView) (mAct.findViewById(R.id.tv_flags));
        msgs.post(new Runnable() {
            @Override
            public void run() {
                msgs.setText(sb);
            }
        });
    }

    @Override
    public void onCtFwSMessage(CtFwSGameState gs, List<CtFwSGameState.Msg> msgs) {
        final TextView msgstv = (TextView) (mAct.findViewById(R.id.msgs));
        int s = msgs.size();

        if (s == 0) {
            msgstv.post(new Runnable() {
                @Override
                public void run() {
                    msgstv.setText("");
                }
            });
        } else {
            CtFwSGameState.Msg m = msgs.get(s - 1);

            long td = (m.when == 0) ? 0 : (gs.isConfigured()) ? m.when - gs.getStartT() : 0;

            final StringBuffer sb = new StringBuffer();
            sb.append(DateUtils.formatElapsedTime(td));
            sb.append(": ");
            sb.append(m.msg);
            sb.append("\n");

            msgstv.post(new Runnable() {
                @Override
                public void run() {
                    msgstv.append(sb);
                }
            });
        }
    }
}