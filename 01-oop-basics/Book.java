class Book {
String title;
String author;
int year;  

Book(String title, String author, int year){
this.title = title;
this.author = author;
this.year = year;
}
  @Override
public String toString(){
  return "Book{title='" + title + "', author='" + author + "', year=" + year + "}";
}  

 @Override 
 public boolean equals(Object obj){
 if (this == obj) return true;

 if (obj == null || getClass() != obj.getClass() ) return false;
 Book other = (Book) obj;
  return title.equals(other.title) && author.equals(other.author) && year == (other.year);

 }

 @Override 
 public int hashCode() {
	 return java.util.Objects.hash(title, author, year);

 }


public static void main(String[] args){
Book b = new Book("think", "odongo odonde", 2026);
Book b2 = new Book("think", "odongo odonde", 2026);
System.out.println(b.hashCode());
System.out.println(b2.hashCode());
System.out.println(b.hashCode() == b2.hashCode());
System.out.println(b.equals(b2));

}
}
