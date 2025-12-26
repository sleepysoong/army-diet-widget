package com.sleepysoong.armydiet.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@SuppressWarnings({"unchecked", "deprecation"})
public final class MealDao_Impl implements MealDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MealEntity> __insertionAdapterOfMealEntity;

  public MealDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMealEntity = new EntityInsertionAdapter<MealEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `meals` (`date`,`breakfast`,`lunch`,`dinner`,`adspcfd`,`sumCal`,`updatedAt`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MealEntity entity) {
        if (entity.getDate() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getDate());
        }
        if (entity.getBreakfast() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getBreakfast());
        }
        if (entity.getLunch() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getLunch());
        }
        if (entity.getDinner() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDinner());
        }
        if (entity.getAdspcfd() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getAdspcfd());
        }
        if (entity.getSumCal() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getSumCal());
        }
        statement.bindLong(7, entity.getUpdatedAt());
      }
    };
  }

  @Override
  public Object insertMeals(final List<MealEntity> meals,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMealEntity.insert(meals);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getMeal(final String date, final Continuation<? super MealEntity> $completion) {
    final String _sql = "SELECT * FROM meals WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MealEntity>() {
      @Override
      @Nullable
      public MealEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfBreakfast = CursorUtil.getColumnIndexOrThrow(_cursor, "breakfast");
          final int _cursorIndexOfLunch = CursorUtil.getColumnIndexOrThrow(_cursor, "lunch");
          final int _cursorIndexOfDinner = CursorUtil.getColumnIndexOrThrow(_cursor, "dinner");
          final int _cursorIndexOfAdspcfd = CursorUtil.getColumnIndexOrThrow(_cursor, "adspcfd");
          final int _cursorIndexOfSumCal = CursorUtil.getColumnIndexOrThrow(_cursor, "sumCal");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final MealEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            final String _tmpBreakfast;
            if (_cursor.isNull(_cursorIndexOfBreakfast)) {
              _tmpBreakfast = null;
            } else {
              _tmpBreakfast = _cursor.getString(_cursorIndexOfBreakfast);
            }
            final String _tmpLunch;
            if (_cursor.isNull(_cursorIndexOfLunch)) {
              _tmpLunch = null;
            } else {
              _tmpLunch = _cursor.getString(_cursorIndexOfLunch);
            }
            final String _tmpDinner;
            if (_cursor.isNull(_cursorIndexOfDinner)) {
              _tmpDinner = null;
            } else {
              _tmpDinner = _cursor.getString(_cursorIndexOfDinner);
            }
            final String _tmpAdspcfd;
            if (_cursor.isNull(_cursorIndexOfAdspcfd)) {
              _tmpAdspcfd = null;
            } else {
              _tmpAdspcfd = _cursor.getString(_cursorIndexOfAdspcfd);
            }
            final String _tmpSumCal;
            if (_cursor.isNull(_cursorIndexOfSumCal)) {
              _tmpSumCal = null;
            } else {
              _tmpSumCal = _cursor.getString(_cursorIndexOfSumCal);
            }
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new MealEntity(_tmpDate,_tmpBreakfast,_tmpLunch,_tmpDinner,_tmpAdspcfd,_tmpSumCal,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getMealsByDates(final List<String> dates,
      final Continuation<? super List<MealEntity>> $completion) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM meals WHERE date IN (");
    final int _inputSize = dates.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (String _item : dates) {
      if (_item == null) {
        _statement.bindNull(_argIndex);
      } else {
        _statement.bindString(_argIndex, _item);
      }
      _argIndex++;
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MealEntity>>() {
      @Override
      @NonNull
      public List<MealEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfBreakfast = CursorUtil.getColumnIndexOrThrow(_cursor, "breakfast");
          final int _cursorIndexOfLunch = CursorUtil.getColumnIndexOrThrow(_cursor, "lunch");
          final int _cursorIndexOfDinner = CursorUtil.getColumnIndexOrThrow(_cursor, "dinner");
          final int _cursorIndexOfAdspcfd = CursorUtil.getColumnIndexOrThrow(_cursor, "adspcfd");
          final int _cursorIndexOfSumCal = CursorUtil.getColumnIndexOrThrow(_cursor, "sumCal");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<MealEntity> _result = new ArrayList<MealEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MealEntity _item_1;
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            final String _tmpBreakfast;
            if (_cursor.isNull(_cursorIndexOfBreakfast)) {
              _tmpBreakfast = null;
            } else {
              _tmpBreakfast = _cursor.getString(_cursorIndexOfBreakfast);
            }
            final String _tmpLunch;
            if (_cursor.isNull(_cursorIndexOfLunch)) {
              _tmpLunch = null;
            } else {
              _tmpLunch = _cursor.getString(_cursorIndexOfLunch);
            }
            final String _tmpDinner;
            if (_cursor.isNull(_cursorIndexOfDinner)) {
              _tmpDinner = null;
            } else {
              _tmpDinner = _cursor.getString(_cursorIndexOfDinner);
            }
            final String _tmpAdspcfd;
            if (_cursor.isNull(_cursorIndexOfAdspcfd)) {
              _tmpAdspcfd = null;
            } else {
              _tmpAdspcfd = _cursor.getString(_cursorIndexOfAdspcfd);
            }
            final String _tmpSumCal;
            if (_cursor.isNull(_cursorIndexOfSumCal)) {
              _tmpSumCal = null;
            } else {
              _tmpSumCal = _cursor.getString(_cursorIndexOfSumCal);
            }
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item_1 = new MealEntity(_tmpDate,_tmpBreakfast,_tmpLunch,_tmpDinner,_tmpAdspcfd,_tmpSumCal,_tmpUpdatedAt);
            _result.add(_item_1);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
