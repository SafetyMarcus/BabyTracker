import UIKit
import SwiftUI
import KMMViewModelSwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    var viewModel: MainViewModel
    var showPicker: ShowDatePicker
    
    init(showPicker: ShowDatePicker) {
        self.showPicker = showPicker
        self.viewModel = self.showPicker.getViewModel()
    }
    
    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.MainViewController(
            viewModel: viewModel,
            showTimePicker: { Child, EventType in
                showPicker.show(child: Child, type: EventType)
            },
            editEvent: { String, Firebase_firestoreTimestamp in
                showPicker.edit(id: String, timestamp: Firebase_firestoreTimestamp)
            },
            deleteEvent: { String in
                showPicker.delete(id: String)
            })
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

protocol ShowDatePicker {
    func show(child: Child, type: EventType)
    func edit(id: String, timestamp: Firebase_firestoreTimestamp)
    func delete(id: String)
    func getViewModel() -> MainViewModel
}

struct ContentView: View, ShowDatePicker {
    
    @State var date: Date = Date()
    @State var pickerShowing: Bool = false
    @State var child: Child? = nil
    @State var type: EventType? = nil
    @State var id: String? = nil
    @State var showingAlert: Bool = false
    @State var deleteEvent: String? = nil
    
    @StateViewModel var model = MainViewModel()
    
    func show(child: Child, type: EventType) {
        self.child = child
        self.type = type
        pickerShowing = true
    }
    
    func edit(id: String, timestamp: Firebase_firestoreTimestamp) {
        self.id = id
        date = Date(timeIntervalSince1970: TimeInterval(timestamp.seconds))
        pickerShowing = true
    }
    
    func delete(id: String) {
        deleteEvent = id
        showingAlert = true
    }

    func getViewModel() -> MainViewModel {
        return model
    }
    
    var body: some View {
        ZStack {
            let composeView = ComposeView(showPicker: self)
            composeView.ignoresSafeArea(.keyboard) // Compose has own keyboard handler
            composeView.alert(Text("Delete"), isPresented: $showingAlert) {
                Button("Cancel") {}
                Button("Delete") {
                    guard let id = deleteEvent else {
                        showingAlert = false
                        return
                    }
                    model.deleteEvent(id: id)
                    showingAlert = false
                }
            } message: {
                Text("Are you sure you want to delete this event?")
            }
            
            if pickerShowing {
                HStack {
                    DatePicker(
                        "",
                        selection: $date,
                        displayedComponents: .hourAndMinute
                    ).datePickerStyle(.automatic)
                    Spacer()
                    Button(
                        "Cancel",
                        action: { pickerShowing = false }
                    ).padding()
                    Button(
                        "Done",
                        action: {
                            pickerShowing = false
                            let time = Firebase_firestoreTimestamp(
                                seconds: Int64(date.timeIntervalSince1970),
                                nanoseconds: 0
                            )
                            guard let id = id else {
                                model.addEvent(
                                    child: child!,
                                    eventType: type!,
                                    time: time
                                )
                                return
                            }
                            model.editEvent(id: id, time: time)
                        }
                    ).padding()
                }.background(Color(UIColor.secondarySystemBackground))
                    .zIndex(2)
            }
        }
    }
}
