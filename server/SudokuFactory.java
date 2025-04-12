public class SudokuFactory {
    public static String[][] createPuzzles() {
        // Example puzzles (you can expand this with more puzzles)
        String[][] puzzles = {
            {
                "387491625",
                "241568379",
                "569327418",
                "758619234",
                "123784596",
                "496253187",
                "934176852",
                "675832941",
                "81294576-"
            },
            {
                "53--7----",
                "6--195---",
                "-98----6-",
                "8---6---3",
                "4--8-3--1",
                "7---2---6",
                "-6----28-",
                "---419--5",
                "----8--79"
            },
            // Add more puzzles here
        };
        return puzzles;
    }

    public static String[][] createSolutions() {
        // Corresponding solutions
        String[][] solutions = {
            {
                "387491625",
                "241568379",
                "569327418",
                "758619234",
                "123784596",
                "496253187",
                "934176852",
                "675832941",
                "812945763"
            },
            {
                "534678912",
                "672195348",
                "198342567",
                "859761423",
                "426853791",
                "713924856",
                "961537284",
                "287419635",
                "345286179"
            },
            // Add more solutions here
        };
        return solutions;
    }
}