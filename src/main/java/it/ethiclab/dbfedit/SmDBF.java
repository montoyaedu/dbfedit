package it.ethiclab.dbfedit;

import java.io.*;
import com.linuxense.javadbf.*;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.*;
import org.xBaseJ.*;

public class SmDBF implements Closeable {
    private static final String enc = "windows-1252";
    private final File dbf;
    private ArrayList<Object[]> records = new ArrayList<>();
    int record;
    int numberOfFields;
    char[] fieldTypes;
    String[] fieldNames;
    int[] fieldLengths;
    int[] fieldDecimals;

    public void delete() throws Exception {
        System.out.println("TODO: should delete row " + record);
        System.out.println("packing...");
        DbfApi api = new DBF(dbf.getAbsolutePath(), enc);
        try {
            api.delete();
        } finally {
            api.close();
        }
        pack();
    }

    public void goToRec(int n) {
        record = n - 1;
    }

    public void pack() throws Exception {
        System.out.println("packing...");
        DbfApi api = new DBF(dbf.getAbsolutePath(), enc);
        try {
            api.pack();
        } finally {
            api.close();
        }
    }

    public void append() throws Exception {
        DbfApi api = new DBF(dbf.getAbsolutePath(), enc);
        try {
            api.write();
        } finally {
            api.close();
        }
    }

    public int getRecCount() {
        return records.size();
    }

    public int getFieldCount() {
        return numberOfFields;
    }

    public char getFieldType(int n) {
        return fieldTypes[n - 1];
    }

    public String getFieldName(int n) {
        return fieldNames[n - 1];
    }

    public int getFieldLength(int n) {
        return fieldLengths[n - 1];
    }

    public int getFieldLengthDecimal(int n) {
        return fieldDecimals[n - 1];
    }

    public void set(int n, Object obj) throws IOException, xBaseJException {
        DbfApi api = new DBF(dbf.getAbsolutePath(), enc);
        try {
            System.out.println("name = " + api.getField(n).getName());
            System.out.println("value before set = " + get(n));
            api.gotoRecord(record + 1);
            if (api.getField(n).isCharField() || api.getField(n).isMemoField()) {
                api.getField(n).put((String) obj);
            } else if (obj != null) {
                api.getField(n).put(obj.toString());
            }
            api.update();
            System.out.println("value after set = " + api.getField(n).get());
            records.get(record)[n - 1] = api.getField(n).get();
        } finally {
            api.close();
        }
    }

    public Object get(int n) throws IOException {
        return records.get(record)[n - 1];
    }

    public SmDBF(File dbf) throws IOException {
        this.dbf = dbf;
        read(dbf);
    }

    public void read(File dbf) throws IOException {
        DBFReader reader = null;
        try {
            reader = new DBFReader(new FileInputStream(dbf), Charset.forName(enc), false);
            String base = dbf.getName().substring(0, dbf.getName().length() - 4);
            String suffix = dbf.getName().substring(dbf.getName().length() - 4);
            char ch = suffix.charAt(suffix.length() - 1);
            suffix = suffix.substring(0, 3) + (Character.isUpperCase(ch) ? 'T' : 't');
            String dbtDefaultName = base + suffix;
            File dbt = new File(
                    Paths.get(dbf.getAbsoluteFile().getParentFile().getAbsolutePath(), dbtDefaultName).toString());
            if (dbt.exists()) {
                reader.setMemoFile(dbt);
            }
            records.clear();
            numberOfFields = reader.getFieldCount();
            int recCount = reader.getRecordCount();
            fieldTypes = new char[numberOfFields];
            fieldNames = new String[numberOfFields];
            fieldLengths = new int[numberOfFields];
            fieldDecimals = new int[numberOfFields];
            for (int i = 0; i < numberOfFields; i++) {
                fieldTypes[i] = (char) reader.getField(i).getDataType();
                fieldNames[i] = reader.getField(i).getName();
                fieldLengths[i] = reader.getField(i).getLength();
                fieldDecimals[i] = reader.getField(i).getDecimalCount();
            }
            for (int i = 0; i < recCount; i++) {
                records.add(reader.nextRecord());
            }
        } finally {
            DBFUtils.close(reader);
        }
    }

    @Override
    public void close() throws IOException {
        // not necessary
    }

    public static String typeToString(char t) {
        return t == 'C' ? "Character"
                : t == 'L' ? "Logical"
                        : t == 'M' ? "Memo"
                                : t == 'D' ? "Date"
                                        : t == 'T' ? "DateTime" : t == 'N' ? "Numeric" : t == 'F' ? "Float" : "";
    }

    public static String rtrim(String s) {
        int i = s.length() - 1;
        while (i >= 0 && s.charAt(i) <= ' ')
            i--;
        return s.substring(0, i + 1);
    }

}
