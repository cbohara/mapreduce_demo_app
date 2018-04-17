// cc MaxTemperature Application to find the maximum temperature in the weather dataset
// vv MaxTemperature
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class MaxTemperature {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: MaxTemperature <input path> <output path>");
            System.exit(-1);
        }

        // input types are not specified because using default TextInputFormat
        Job job = new Job();
        // hadoop will find JAR associated with class
        job.setJarByClass(MaxTemperature.class);
        job.setJobName("Max temperature");

        // can have multiple input paths
        FileInputFormat.addInputPath(job, new Path(args[0]));
        // only single output path
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // set mapper and reducer
        job.setMapperClass(MaxTemperatureMapper.class);
        job.setReducerClass(MaxTemperatureReducer.class);

        // specify output types
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
// ^^ MaxTemperature