package com.realtime.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.realtime.Constants;

/**
 * <PRE>
 * Filename : StringUtil.java
 * Class    : StringUtil
 * Function :
 * Comment  :
 *
 * Copyright (c) 2016 by exanadu Corp. All Rights Reserved.
 * </PRE>
 *
 * @since 2016-11-23
 * @author fixalot
 */
public class StringUtil {
	private static final Logger log = LoggerFactory.getLogger(StringUtil.class);
	
	/**
	 * text의 바이트 크기 계산. 
	 * ASCII 코드에 해당하면 1, 그 외는 모두 2로 계산하도록 되어 있다.
	 * 
	 * @param text
	 * @return
	 * @author fixalot
	 */
	public static int getByteSize(String text) {
		int length = 0;
		if (text == null || text.isEmpty()) {
			return 0;
		}
		for (int i = 0; i < text.length(); i++) {
			final char ch = text.charAt(i);
			if (ch > 127) {
				length += 2;
			} else {
				length += 1;
			}
		}
		return length;
	}

	/**
     * 문자열에서 특정 문자열을 치환한다.
     * @return the translated string.
     * @param source String 변환할 문자열
     * @param keyStr String 치환 대상 문자열
     * @param toStr String 치환될 문자열 예를 들어, 123456-7890123라는 문자열 str을
     * 1234567890123 형식으로 바꾸고 싶다면, replaceStr( str, "-", "") 로 호출한다.
     */
    public static String replaceStr( String source, String keyStr, String toStr ) {
        if ( source == null )
            return null;
        int startIndex = 0;
        int curIndex = 0;
        StringBuffer result = new StringBuffer();

        while ( ( curIndex = source.indexOf( keyStr, startIndex ) ) >= 0 ) {
            result.append( source.substring( startIndex, curIndex ) ).append( toStr );
            startIndex = curIndex + keyStr.length();
        }

        if ( startIndex <= source.length() )
            result.append( source.substring( startIndex, source.length() ) );

        return result.toString();

    }

    /**
     * pad 함수
     *
     * @param str   대상문자열, len 길이, addStr 대체문자
     * @return      문자열
     */
    public static String lpad(String str, int len, String addStr) {
        if ( str == null ) {
            str = "";
        }
        String result = str;
        int templen   = len - result.length();

        for (int i = 0; i < templen; i++){
              result = addStr + result;
        }

        return result;
    }

    /**
     * pad 함수
     *
     * @param str   대상문자열, len 길이, addStr 대체문자
     * @return      문자열
     */
    public static String rpad(String str, int len, String addStr) {
        if ( str == null ) {
            str = "";
        }
        String result = str;
        int templen   = len - result.length();

        for (int i = 0; i < templen; i++){
              result = result + addStr;
        }

        return result;
    }

    /**
     * pad 함수
     *
     * @param str   대상문자열, len 길이, addStr 대체문자
     * @return      문자열
     */
    public static String lpadByte(String str, int len, String addStr) {
        if ( str == null ) {
            str = "";
        }
        String result = str;
        int templen   = len - result.getBytes().length;

        for (int i = 0; i < templen; i++){
              result = addStr + result;
        }

        return result;
    }

    /**
     * pad 함수
     *
     * @param str   대상문자열, len 길이, addStr 대체문자
     * @return      문자열
     */
    public static String rpadByte(String str, int len, String addStr) {
        if ( str == null ) {
            str = "";
        }
        String result = str;
        int templen   = len - result.getBytes().length;

        for (int i = 0; i < templen; i++){
              result = result + addStr;
        }

        return result;
    }





    /**
     * nvl 함수
     *
     * @param str   대상문자열
     * @return      문자열
     */
    public static String nvl(String str) {
        if ( str == null ) {
            str = "";
        }
        return str;
    }

    /**
     * nvl 함수
     */
    public static String nvl(String str, String req) {
        if ( str == null || "".equals(str.trim()) ) {
            str = req;
        }
        return str;
    }
    /**
     * parseStringByBytes 함수
     *
     * @param str   대상문자열, len 길이
     * @return      String[]
     */
    public static String[] parseStringByBytes(String raw, int len, String encoding) {
	  	  if (raw == null) return null;

	  	  String[] ary = null;

	  	  try {
	  		  byte[] rawBytes = raw.getBytes(encoding);

	  		  int rawLength = rawBytes.length;
	  		  int index = 0;
	  		  int minus_byte_num = 0;
	  		  int offset = 0;
	  		  int hangul_byte_num = encoding.equals("UTF-8") ? 3 : 2;
	  		  if(rawLength > len){
	  			  int aryLength = (rawLength / len) + (rawLength % len != 0 ? 1 : 0);
	  			  ary = new String[aryLength];
	  			  for(int i=0; i<aryLength; i++){
	  				  minus_byte_num = 0;
	  				  offset = len;
	  				  if(index + offset > rawBytes.length){
	  					  offset = rawBytes.length - index;
	  				  }

	  				  for(int j=0; j<offset; j++){
	  					  if((rawBytes[index + j] & 0x80) != 0){
	  						  minus_byte_num ++;
	  					  }
	  				  }
	  				  if(minus_byte_num % hangul_byte_num != 0){
	  					  offset -= minus_byte_num % hangul_byte_num;
	  				  }
	  				  ary[i] = new String(rawBytes, index, offset, encoding);
	  				  index += offset ;
	  			  }
	  		  } else {
	  			  ary = new String[]{raw};
	  		  }
	  	  } catch(Exception e) {

	  	  }
	  	  return ary;
	  }

    /**
	 * 파라메터가 null 인 경우 빈문자열을 반환.
	 *
	 * @param <T>
	 * @param arg
	 * @return String
	 */
	public static <T> String defaultString(T arg) {
		return defaultString(arg, "");
	}

	/**
	 * 파라메터가 null 인 경우 초기화 문자열을 반환, null 이 아닌경우 trim 된 문자열을 반환.
	 *
	 * @param <T>
	 * @param arg
	 * @param init
	 * @return String
	 */
	public static <T> String defaultString(T arg, T init) {
		String str = parseString(arg);

		if ("".equals(str)){
			return parseString(init);
		}

		return str;
	}
	
	/**
	 * 문자열 null체크
	 * 
	 * @param str 문자열
	 * @return 문자열 리턴 (null 또는 공백 일때 공백리턴)
	 */
	public static String defString(String str) {
		return defString(str, "");
	}

	/**
	 * 문자열 null체크 후 기본값 적용
	 * 
	 * @param str 문자열
	 * @param def 기본값
	 * @return 문자열 리턴 (null 또는 공백일때 기본값리턴)
	 */
	public static String defString(String str, String def) {
		if (!StringUtils.hasText(str)) {
			str = def;
		}
		return str;
	}

	/**
	 * 자료형을 String Type 으로 변환한다.
	 *
	 * @param <T>
	 * @param arg
	 * @return String
	 */
	public static <T> String parseString(T arg) {
		if (arg == null)
			return "";

		return (String.valueOf(arg)).trim();
	}

	public static String getStackTrace(Throwable throwable) {
	    String result = "";
	    Writer writer = null;
	    PrintWriter printWriter = null;
		try {
			writer = new StringWriter();
			printWriter = new PrintWriter(writer);
			throwable.printStackTrace(printWriter);
			result = writer.toString();
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}finally{
			try {
				if(writer != null){
					writer.close();
				}
				if(printWriter != null){
					printWriter.close();
				}
			}catch (IOException ie) {
				log.error(ie.getMessage(),ie);
			}
		}
	    return result;
	}

	/**
	 * 돈표시 변환
	 *
	 * @param <T>
	 * @param arg
	 * @return String
	 * @throws Exception
	 */
	public static <T> String moneyFormat(T arg) {
		String str = StringUtil.parseString(arg);

		str = Pattern.compile("[^0-9]", Pattern.CASE_INSENSITIVE).matcher(str)
				.replaceAll("");

		return (new DecimalFormat("###,###,###")).format(StringUtil
				.intValue(str));
	}

	/**
	 * 입력 받은 파라메터를 INTEGER TYPE 으로 변환한다. NULL 이거나 아무런 값이 없는 경우 0 이 반환된다.
	 *
	 * @param <T>
	 * @param arg
	 * @return Integer
	 */
	public static <T> int intValue(T arg) {
		return intValue(arg, 0);
	}

	/**
	 * 입력 받은 파라메터를 INTEGER TYPE 으로 변환한다. NULL 이거나 아무런 값이 없는 경우 초기화 파라메터값으로 반환된다.
	 * NumberFormatException 발생시 초기화 파라메터 값으로 반환.
	 *
	 * @param <T>
	 * @param arg
	 * @param init
	 * @return Integer
	 */
	public static <T> int intValue(T arg, int init) {
		String str = defaultString(parseString(arg), init);

		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			log.error(e.getMessage(),e);
		}

		return init;
	}

	/**
	 * 주어진 문자열을 길이 1333 기준으로 split한다.
	 *
	 * @param str 자를 문자열
	 * @return
	 * @author fixalot
	 */
	public static String[] splitByLength1333(String str) {
		return splitByLength(str, 1333);
	}

	/**
	 * 지정된 길이만큼 str을 split한다.
	 *
	 * @param str 자를 문자열
	 * @param splitLength 문자열을 자를 길이
	 * @return String Array
	 * @author fixalot
	 */
	public static String[] splitByLength(String str, int splitLength) {
		if (str == null) {
			return null;
		}

		int strLen = str.length();
		int arrayLength = Math.abs(strLen / splitLength) + (strLen % splitLength != 0 ? 1 : 0);

		String[] strArray = null;
		if (arrayLength == 0) {
			return new String[] { str };
		} else {
			strArray = new String[arrayLength];
		}

		String temp = "";
		for (int i = 0; i < arrayLength; i++) {
			if (str.length() > splitLength) {
				strArray[i] = str.substring(0, splitLength);
				temp = str.substring(splitLength, str.length());
			} else {
				strArray[i] = str.substring(0, str.length());
			}
			str = temp;
		}
		return strArray;
	}

	/**
	 * 문자열 byte로 잘라 배열 리턴
	 * 
	 * @param str : 지정 문자열
	 * @param len : 지정 바이트
	 * @return
	 * @author soom
	 */
	public static String[] cutString(String str, int len) {
		if (str == null) {
			return null;
		}
		String[] arr = null;
		try {
			byte[] strBytes = str.getBytes("UTF-8");
			int strLength = strBytes.length;

			if (strLength > len) {
				int arrLength = (strLength / len) + (strLength % len != 0 ? 1 : 0);
				arr = new String[arrLength];

				int endCharIndex = 0; // 문자열이 끝나는 위치
				String tmp;
				for (int i = 0; i < arrLength; i++) {
					if (i == (arrLength - 1)) {
						tmp = str.substring(endCharIndex);
					} else {
						int useByteLength = 0;
						int rSize = 0;
						for (; endCharIndex < str.length(); endCharIndex++) {
							if (str.charAt(endCharIndex) > 0x007F) {
								useByteLength += 3;
							} else {
								useByteLength++;
							}
							if (useByteLength > len) {
								break;
							}
							rSize++;
						}
						tmp = str.substring((endCharIndex - rSize), endCharIndex);
					}
					arr[i] = tmp;
				}
			} else {
				arr = new String[] { str };
			}
		} catch (Exception e) {
		}

		return arr;
	}
	
	/**
	 * 대상문자열(strTarget)에서 특정문자열(strSearch)을 찾아 지정문자열(strReplace)로
	 * 변경한 문자열을 반환한다.
	 *
	 * @param strTarget 대상문자열
	 * @param strSearch 변경대상의 특정문자열
	 * @param strReplace 변경 시키는 지정문자열
	 * @return 변경완료된 문자열
	 */
	public static String replaceStr2(String strTarget, String strSearch, String strReplace) {
		//null 처리
		if (strTarget == null) {
			return "";
		}
		String strCheck = strTarget;
		StringBuffer strBuf = new StringBuffer();
		while(strCheck.length() != 0) {
			int begin = strCheck.indexOf(strSearch);
			if(begin == -1) {
				strBuf.append(strCheck);
				break;
			} else {
				int end = begin + strSearch.length();
				strBuf.append(strCheck.substring(0, begin));
				strBuf.append(strReplace);
				strCheck = strCheck.substring(end);
			}
		}
		return new String(strBuf);
	}
	
	
	/**
	 * 특수문자 HTML태그로 변경
	 * @param str
	 * @return
	 */
	public static String strToHtml (String str) {
		str = replaceStr(str, "<","&#60;");
		str = replaceStr(str, ">","&#62;");
		str = replaceStr(str, "(", "&#40;");
		str = replaceStr(str, ")", "&#41;");
		str = replaceStr(str, "'", "&#39;");
		str = replaceStr(str, "\"", "&#34;");
		str = replaceStr(str, ",", "&#44;");
		return str;		
	}
	
	/**
	 * 특수문자 HTML태그로 변경
	 * @param str
	 * @return
	 */
	public static String htmlToStr (String html) {
		html = replaceStr(html,"&#60;", "<");
		html = replaceStr(html,"&#62;", ">");
		html = replaceStr(html, "&#40;", "(");
		html = replaceStr(html, "&#41;", ")");
		html = replaceStr(html, "&#39;", "'");
		html = replaceStr(html, "&#34;", "\"");
		html = replaceStr(html, "&#44;", ",");
		return html;		
	}
	
	/**
	 * 양쪽 공백 제거 (null일때 공백리턴)
	 * 
	 * @param str 문자열
	 * @return 양쪽 공백 제거 (null일때 공백리턴)
	 */
	public static String trimWhitespace(String str) {
		str = StringUtils.trimWhitespace(str);
		return defString(str);
	}
	
	public static String trimWhitespace(byte[] bytes, int startPosition, int length) throws UnsupportedEncodingException {
		String str = new java.lang.String(bytes, startPosition,length, Constants.ENCODE_TYPE);
		str = StringUtils.trimWhitespace(str);
		return defString(str);
	}
	
	public static int trimWhitespaceToInt(byte[] bytes, int startPosition, int length) throws UnsupportedEncodingException {
		String str = new java.lang.String(bytes, startPosition,length, Constants.ENCODE_TYPE);
		str = StringUtils.trimWhitespace(str);
		str = defString(str);
		
		return Integer.parseInt(str.replaceAll("^0+",""));
	}
}

