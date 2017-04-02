package br.com.netomarin.iot.ledstripgame;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;

import com.google.android.things.contrib.driver.apa102.Apa102;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;

import java.io.IOException;

public class LedStripGame extends Activity {

    private static final String TAG = LedStripGame.class.getSimpleName();

    private static final int LED_DIRECTION_ASC = 1;
    private static final int LED_DIRECTION_DESC = 0;
    private static final int LED_STRIP_SIZE = 7;
    private static final long INITIAL_INTERVAL_BETWEEN_BLINKS_MS = 500;
    private static final int[] POINT_LED_COLORS = { Color.GREEN, Color.GREEN, Color.GREEN,
            Color.GREEN, Color.GREEN, Color.GREEN, Color.GREEN};
    private static final int[] MISSED_LED_COLORS = { Color.RED, Color.RED, Color.RED, Color.RED,
            Color.RED, Color.RED, Color.RED};
    private static final int[] LVLUP_LED_COLORS = { Color.YELLOW, Color.YELLOW, Color.YELLOW,
            Color.YELLOW, Color.YELLOW, Color.YELLOW, Color.YELLOW};
    private static final int[] BLANK_LED_COLORS = new int[7];

    private Handler mHandler = new Handler();

    private ButtonInputDriver mButtonB;
    private ButtonInputDriver mButtonC;
    private AlphanumericDisplay mDisplay;
    private Apa102 mLedStrip;
    private int[] mLedColors = new int[LED_STRIP_SIZE];

    private int mLedCyle;
    private int mLedDirection;

    private boolean mGameRunning;
    private int mLevel;
    private long mCurrentInterval;
    private int mScore;
    private int mLastCycle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        mLastCycle = mLedCyle = 0;
        mLedDirection = LED_DIRECTION_ASC;
        mScore = 0;
        mLevel = 0;
        mCurrentInterval = INITIAL_INTERVAL_BETWEEN_BLINKS_MS;
        mGameRunning = false;

        try {
            mButtonB = new ButtonInputDriver("BCM20",
                    Button.LogicState.PRESSED_WHEN_LOW, KeyEvent.KEYCODE_B);
            mButtonB.register();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            mButtonC = new ButtonInputDriver("BCM16",
                    Button.LogicState.PRESSED_WHEN_LOW, KeyEvent.KEYCODE_C);
            mButtonC.register();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            mDisplay = new AlphanumericDisplay("I2C1");
            mDisplay.setEnabled(true);
            mDisplay.clear();
            mDisplay.display(mScore);
        } catch (IOException e) {
            Log.e(TAG, "Erro inicializando display");
            mDisplay = null;
        }

        try {
            mLedStrip = new Apa102("SPI0.0", Apa102.Mode.BGR);
            mLedStrip.setBrightness(1);
            mLedStrip.write(BLANK_LED_COLORS);
        } catch (IOException e) {
            Log.e(TAG, "Erro inicializando leds");
            e.printStackTrace();
            mLedStrip = null;
        }

        mHandler.post(mGameLoopRunnable);
        startLevel();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        try {
            mDisplay.clear();
            if(keyCode == KeyEvent.KEYCODE_B) {
                mGameRunning = false;
                checkRoundResult();
            } else if (keyCode == KeyEvent.KEYCODE_C) {
                resetGame();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void resetGame() {
        try {
            mDisplay.clear();
            mDisplay.display("RST");
            mLedStrip.write(MISSED_LED_COLORS);
            Thread.sleep(300);
            mLedStrip.write(BLANK_LED_COLORS);
            Thread.sleep(300);
            mLedStrip.write(MISSED_LED_COLORS);
            Thread.sleep(300);
            mLedStrip.write(BLANK_LED_COLORS);
            Thread.sleep(300);
            mScore = 0;
            mLevel = 0;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        startLevel();
    }

    private void checkRoundResult() {
        if ( (mLedDirection == LED_DIRECTION_ASC && mLedCyle - 2 == 3) ||
                (mLedDirection == LED_DIRECTION_DESC && mLedCyle + 2 == 3)) {
            //POINT!!
            mScore++;
            blinkRound(true);
            checkLevel();
        } else {
            blinkRound(false);
            startGameRound();
        }
    }

    private void blinkRound(boolean point) {
        try {
            mDisplay.display(point ? "YES" : "NOOO");
            mLedStrip.write(point ? POINT_LED_COLORS : MISSED_LED_COLORS);
            Thread.sleep(500);
            mLedStrip.write(BLANK_LED_COLORS);
            Thread.sleep(300);
            mLedStrip.write(point ? POINT_LED_COLORS : MISSED_LED_COLORS);
            Thread.sleep(500);
            mLedStrip.write(BLANK_LED_COLORS);
            Thread.sleep(300);
            mLedStrip.write(point ? POINT_LED_COLORS : MISSED_LED_COLORS);
            Thread.sleep(300);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void checkLevel() {
        if (mScore % 5 == 0) {
            mLevel = mScore / 5;
            if (mLevel < 9) {
                try {
                    mDisplay.clear();
                    mDisplay.display('U', 1, false);
                    mDisplay.display('P', 2, false);
                    mLedStrip.write(LVLUP_LED_COLORS);
                    Thread.sleep(300);
                    mLedStrip.write(BLANK_LED_COLORS);
                    Thread.sleep(300);
                    mLedStrip.write(LVLUP_LED_COLORS);
                    Thread.sleep(300);
                    mLedStrip.write(BLANK_LED_COLORS);
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mCurrentInterval = INITIAL_INTERVAL_BETWEEN_BLINKS_MS - (50 * mLevel);
                Log.d(TAG, "New speed: " + mCurrentInterval);
                startLevel();
            } else {
                try {
                    mDisplay.display("WIN");
                    mLedStrip.write(LVLUP_LED_COLORS);
                    Thread.sleep(300);
                    mLedStrip.write(BLANK_LED_COLORS);
                    Thread.sleep(300);
                    mLedStrip.write(POINT_LED_COLORS);
                    Thread.sleep(300);
                    mLedStrip.write(BLANK_LED_COLORS);
                    Thread.sleep(300);
                    mLedStrip.write(LVLUP_LED_COLORS);
                    Thread.sleep(300);
                    mLedStrip.write(BLANK_LED_COLORS);
                    Thread.sleep(300);
                    mLedStrip.write(POINT_LED_COLORS);
                    Thread.sleep(300);
                    mLedStrip.write(BLANK_LED_COLORS);
                    Thread.sleep(300);
                    mLedStrip.write(LVLUP_LED_COLORS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            startGameRound();
        }
    }

    private void startLevel() {
        mGameRunning = false;
        try {
            mDisplay.clear();
            mDisplay.display("LVL" + mLevel);
            Thread.sleep(500);
            mDisplay.clear();
            Thread.sleep(300);
            mDisplay.display("LVL" + mLevel);
            Thread.sleep(500);
            mDisplay.clear();
            Thread.sleep(300);
            mDisplay.display('G', 1, false);
            mDisplay.display('O', 2, false);
            Thread.sleep(500);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        startGameRound();
    }

    private void startGameRound() {
        mLastCycle = mLedCyle = 0;
        mLedDirection = LED_DIRECTION_ASC;
        try {
            mDisplay.display(mScore);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mGameRunning = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mButtonB != null) {
            try {
                mButtonB.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mButtonB = null;
        }

        if(mButtonC != null) {
            try {
                mButtonC.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mButtonC = null;
        }

        if (mLedStrip != null) {
            try {
                mLedStrip.write(new int[7]);
                mLedStrip.setBrightness(0);
                mLedStrip.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mLedStrip = null;
            }
        }
    }

    private Runnable mGameLoopRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mGameRunning) {
                return;
            }

            for (int i = 0; i < LED_STRIP_SIZE; i++) {
                if (i == mLedCyle) {
                    mLedColors[i] = Color.BLUE;
                } else {
                    mLedColors[i] = 0;
                }
            }

            try {
                mLedStrip.write(mLedColors);
                mLastCycle = mLedCyle;
                Log.d(TAG, "LED: " + mLastCycle);
            } catch (IOException e) {
                Log.e(TAG, "Erro ao ligar LEDs");
                return;
            }

            if (mLedDirection == LED_DIRECTION_ASC) {
                if (mLedCyle == LED_STRIP_SIZE - 1) {
                    mLedCyle--;
                    mLedDirection = LED_DIRECTION_DESC;
                } else {
                    mLedCyle++;
                }
            } else {
                if (mLedCyle == 0) {
                    mLedCyle++;
                    mLedDirection = LED_DIRECTION_ASC;
                } else {
                    mLedCyle--;
                }
            }

            mHandler.postDelayed(mGameLoopRunnable, mCurrentInterval);
        }
    };
}